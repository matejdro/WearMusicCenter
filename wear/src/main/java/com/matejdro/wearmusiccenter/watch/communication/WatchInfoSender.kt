package com.matejdro.wearmusiccenter.watch.communication

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.support.wearable.input.WearableButtons
import android.view.KeyEvent
import android.view.WindowManager
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.wearable.input.WearableInputDevice
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.SpecialButtonCodes
import com.matejdro.wearmusiccenter.proto.WatchInfo
import com.matejdro.wearutils.miscutils.BitmapUtils

class WatchInfoSender(val context: Context, val urgent: Boolean) {
    lateinit var googleApiClient: GoogleApiClient

    fun sendWatchInfoToPhone() {
        googleApiClient.connect()
    }

    private val connectionCallback = object : GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(p0: Bundle?) {
            val builder = WatchInfo.newBuilder()

            val putDataRequest = PutDataRequest.create(CommPaths.DATA_WATCH_INFO)

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val displaySize = Point()
            display.getSize(displaySize)

            builder.time = System.currentTimeMillis()
            builder.roundWatch = context.resources.configuration.isScreenRound
            builder.displayDensity = context.resources.displayMetrics.density
            builder.displayWidth = displaySize.x
            builder.displayHeight = displaySize.y

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Button count includes non-customizable primary button. Subtract one.
                val buttons = getAvailableButtonsOnWatch(context)
                        .map { buttonCode ->
                            val buttonLabel: CharSequence?
                            val imageBytes: ByteArray?
                            var supportsLongPress = true

                            when (buttonCode) {
                                in KeyEvent.KEYCODE_STEM_1..KeyEvent.KEYCODE_STEM_3 -> {
                                    buttonLabel = WearableButtons.getButtonLabel(context, buttonCode)
                                    val buttonImage = WearableButtons.getButtonIcon(context, buttonCode)
                                    imageBytes = BitmapUtils.serialize(BitmapUtils.getBitmap(buttonImage))
                                }
                                KeyEvent.KEYCODE_BACK -> {
                                    buttonLabel = context.getString(R.string.back_button)
                                    imageBytes = null
                                }
                                SpecialButtonCodes.TURN_ROTARY_CW -> {
                                    buttonLabel = context.getString(R.string.turn_bezel_cw)
                                    imageBytes = null
                                    supportsLongPress = false
                                }
                                SpecialButtonCodes.TURN_ROTARY_CCW -> {
                                    buttonLabel = context.getString(R.string.turn_bezel_ccw)
                                    imageBytes = null
                                    supportsLongPress = false
                                }
                                else -> {
                                    buttonLabel = null
                                    imageBytes = null
                                }
                            }

                            if (imageBytes != null) {
                                putDataRequest.putAsset(
                                        CommPaths.ASSET_WATCH_INFO_BUTTON_PREFIX + "/" + buttonCode,
                                        Asset.createFromBytes(imageBytes)
                                )
                            }

                            WatchInfo.WatchButton
                                    .newBuilder()
                                    .setLabel(buttonLabel?.toString() ?: context.getString(R.string.button))
                                    .setCode(buttonCode)
                                    .setSupportsLongPress(supportsLongPress)
                                    .build()
                        }

                builder.addAllButtons(buttons)
            }

            if (urgent) {
                putDataRequest.setUrgent()
            }

            putDataRequest.data = builder.build().toByteArray()
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).setResultCallback {
                googleApiClient.disconnect()
            }
        }

        override fun onConnectionSuspended(p0: Int) {
        }
    }

    init {
        googleApiClient = GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(connectionCallback)
                .build()
    }

    companion object {
        fun getAvailableButtonsOnWatch(context: Context): List<Int> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return emptyList()
            }

            return buildList {
                addAll(
                        WearableInputDevice.getAvailableButtonKeyCodes(context)
                        .filter { it in KeyEvent.KEYCODE_STEM_1..KeyEvent.KEYCODE_STEM_3 }
                )

                // Samsung watches have extra hardware back button
                if (Build.BRAND == "samsung") {
                    add(KeyEvent.KEYCODE_BACK)
                }

                if (hasDiscreteRotaryInput()) {
                    add(SpecialButtonCodes.TURN_ROTARY_CW)
                    add(SpecialButtonCodes.TURN_ROTARY_CCW)
                }
            }

        }

        /**
         * Returns *true* if device has a rotary input that moves in discrete steps (for example, notches on rotating bezel)
         *
         * this kind of input is treated like a button instead of as a rotary input, which allows for more customization
         */
        fun hasDiscreteRotaryInput(): Boolean {
            return Build.BRAND == "samsung" && Build.DEVICE.startsWith("wise")
        }
    }
}
