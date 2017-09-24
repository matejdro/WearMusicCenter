package com.matejdro.wearmusiccenter.config.buttons

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.annotation.WorkerThread
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.actions.StandardIcons
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.proto.WatchActions
import com.matejdro.wearutils.miscutils.BitmapUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class WatchConfigSender(actionConfigStorage: ActionConfigStorage,
                        private val context: Context,
                        private val watchInfoProvider: WatchInfoProvider,
                        private val endpointPath: String) {
    private val apiClient: GoogleApiClient = GoogleApiClient.Builder(context)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    resendIfNeeded(actionConfigStorage)
                }

                override fun onConnectionSuspended(p0: Int) = Unit
            })
            .addApi(Wearable.API)
            .build()

    init {
        apiClient.connect()
    }

    private fun resendIfNeeded(actionConfigStorage: ActionConfigStorage) {
        launch(CommonPool) {
            val anyDataOnWatch = Wearable.DataApi.getDataItems(apiClient,
                    Uri.parse("wear://*$endpointPath"))
                    .await()
                    .any()

            if (!anyDataOnWatch) {
                sendConfigToWatch(actionConfigStorage.getAllActions())
            }
        }
    }

    @WorkerThread
    fun sendConfigToWatch(buttons: Collection<Map.Entry<ButtonInfo, PhoneAction>>): Boolean {
        val connectionStatus = apiClient.blockingConnect()
        if (!connectionStatus.isSuccess) {
            GoogleApiAvailability.getInstance().showErrorNotification(context, connectionStatus)
            return false
        }

        val density = watchInfoProvider.value?.watchInfo?.displayDensity ?: 1f
        val targetIconSize = (ConfigConstants.ICON_SIZE_DP * density).toInt()

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


            var icon = BitmapUtils.getBitmap(action.getIcon())
            icon = BitmapUtils.shrinkPreservingRatio(icon, targetIconSize, targetIconSize, true)

            val iconData = BitmapUtils.serialize(icon)
            val assetKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + buttonInfo.getKey()
            putDataRequest.putAsset(assetKey, Asset.createFromBytes(iconData))
        }

        putDataRequest.data = protoBuilder.build().toByteArray()

        val result = Wearable.DataApi.putDataItem(apiClient, putDataRequest).await()
        return result.status.isSuccess
    }

    private val volumeStep by lazy {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val volumeSteps = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumeStep = 1f / volumeSteps

        volumeStep
    }
}