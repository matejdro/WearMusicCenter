package com.matejdro.wearmusiccenter.config.buttons

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.actions.StandardIcons
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.proto.WatchActions
import com.matejdro.wearmusiccenter.util.launchWithPlayServicesErrorHandling
import com.matejdro.wearutils.miscutils.BitmapUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.tasks.await

@AutoFactory
class ButtonConfigTransmitter(buttonConfig: ButtonConfig,
                              @Provided private val context: Context,
                              @Provided private val watchInfoProvider: WatchInfoProvider,
                              @Provided private val customIconStorage: CustomIconStorage,
                              private val endpointPath: String) {

    private val dataClient = Wearable.getDataClient(context)

    init {
        resendIfNeeded(buttonConfig)
    }

    private fun resendIfNeeded(buttonConfig: ButtonConfig) {
        GlobalScope.launchWithPlayServicesErrorHandling(context) {
            val dataOnWatch = dataClient.getDataItems(Uri.parse("wear://*$endpointPath")).await()

            if (!dataOnWatch.any()) {
                sendConfigToWatch(buttonConfig.getAllActions())
            }

            dataOnWatch.release()
        }
    }

    suspend fun sendConfigToWatch(buttons: Collection<Map.Entry<ButtonInfo, PhoneAction>>) {
        val density = watchInfoProvider.value?.watchInfo?.displayDensity ?: 1f
        val targetIconSize = (ConfigConstants.BUTTON_ICON_SIZE_DP * density).toInt()

        val putDataRequest = PutDataRequest.create(endpointPath)
        val protoBuilder = WatchActions.newBuilder()
        protoBuilder.volumeStep = volumeStep

        for ((buttonInfo, action) in buttons) {
            val buttonInfoProto = buttonInfo.buildProtoVersion()
            buttonInfoProto.actionKey = action.javaClass.canonicalName
            protoBuilder.addActions(buttonInfoProto.build())

            if (buttonInfo.physicalButton) {
                // No need to send action images for physical buttons
                continue
            }

            if (action.customIconUri == null &&
                    StandardIcons.hasIcon(buttonInfoProto.actionKey)) {
                // We already have vector icon of this on the watch.
                // No need to waste bluetooth bandwith by transferring it

                continue
            }


            var icon = BitmapUtils.getBitmap(customIconStorage[action])
            icon = BitmapUtils.shrinkPreservingRatio(icon, targetIconSize, targetIconSize, true)

            val iconData = BitmapUtils.serialize(icon)
            val assetKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + buttonInfo.getKey()
            if (iconData != null) {
                putDataRequest.putAsset(assetKey, Asset.createFromBytes(iconData))
            }
        }

        putDataRequest.data = protoBuilder.build().toByteArray()

        dataClient.putDataItem(putDataRequest).await()
    }

    private val volumeStep by lazy {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val volumeSteps = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumeStep = 1f / volumeSteps

        volumeStep
    }
}
