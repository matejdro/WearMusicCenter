package com.matejdro.wearmusiccenter.config.actionlist

import android.content.Context
import android.support.annotation.WorkerThread
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.actions.StandardIcons
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.config.buttons.ConfigConstants
import com.matejdro.wearmusiccenter.proto.WatchList
import com.matejdro.wearutils.miscutils.BitmapUtils

class WatchActionListSender(private val context: Context, private val watchInfoProvider: WatchInfoProvider) {
    val apiClient: GoogleApiClient = GoogleApiClient.Builder(context)
            .addApi(Wearable.API)
            .build()

    init {
        apiClient.connect()
    }

    @WorkerThread
    fun sendConfigToWatch(actions: List<PhoneAction>): Boolean {
        val connectionStatus = apiClient.blockingConnect()
        if (!connectionStatus.isSuccess) {
            GoogleApiAvailability.getInstance().showErrorNotification(context, connectionStatus)
            return false
        }

        val density = watchInfoProvider.value?.watchInfo?.displayDensity ?: 1f
        val targetIconSize = (ConfigConstants.ICON_SIZE_DP * density).toInt()

        val putDataRequest = PutDataRequest.create(CommPaths.DATA_LIST_ITEMS)
        val protoBuilder = WatchList.newBuilder()

        for ((index, action) in actions.withIndex()) {
            val actionProto = WatchList.WatchListAction.newBuilder()
            actionProto.actionTitle = action.getTitle()
            actionProto.actionKey = action.javaClass.canonicalName
            protoBuilder.addActions(actionProto.build())

            if (action.customIconUri == null &&
                    StandardIcons.hasIcon(actionProto.actionKey)) {
                // We already have vector icon of this on the watch.
                // No need to waste bluetooth bandwith by transferring it

                continue
            }

            var icon = BitmapUtils.getBitmap(action.getIcon())
            icon = BitmapUtils.shrinkPreservingRatio(icon, targetIconSize, targetIconSize, true)

            val iconData = BitmapUtils.serialize(icon)
            val assetKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + index
            putDataRequest.putAsset(assetKey, Asset.createFromBytes(iconData))
        }

        putDataRequest.data = protoBuilder.build().toByteArray()

        val result = Wearable.DataApi.putDataItem(apiClient, putDataRequest).await()
        return result.status.isSuccess
    }
}