package com.matejdro.wearmusiccenter.config.actionlist

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.actions.StandardIcons
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.config.buttons.ConfigConstants
import com.matejdro.wearmusiccenter.proto.WatchList
import com.matejdro.wearutils.coroutines.await
import com.matejdro.wearutils.miscutils.BitmapUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AutoFactory
class ActionListTransmitter(actionList: ActionList,
                            @Provided private val customIconStorage: CustomIconStorage,
                            @Provided private val context: Context,
                            @Provided private val watchInfoProvider: WatchInfoProvider) {

    private val dataClient = Wearable.getDataClient(context)

    init {
        resendIfNeeded(actionList)
    }

    private fun resendIfNeeded(actionList: ActionList) {
        GlobalScope.launch {
            val dataOnWatch = dataClient.getDataItems(Uri.parse("wear://*${CommPaths.DATA_LIST_ITEMS}")).await()

            if (!dataOnWatch.any()) {
                sendConfigToWatch(actionList.actions)
            }

            dataOnWatch.release()
        }
    }


    suspend fun sendConfigToWatch(actions: List<PhoneAction>) {
        val density = watchInfoProvider.value?.watchInfo?.displayDensity ?: 1f
        val targetIconSize = (ConfigConstants.MENU_ICON_SIZE_DP * density).toInt()

        val putDataRequest = PutDataRequest.create(CommPaths.DATA_LIST_ITEMS)
        val protoBuilder = WatchList.newBuilder()

        for ((index, action) in actions.withIndex()) {
            val actionProto = WatchList.WatchListAction.newBuilder()
            actionProto.actionTitle = action.title
            actionProto.actionKey = action.javaClass.canonicalName
            protoBuilder.addActions(actionProto.build())

            if (action.customIconUri == null &&
                    StandardIcons.hasIcon(actionProto.actionKey)) {
                // We already have vector icon of this on the watch.
                // No need to waste bluetooth bandwith by transferring it

                continue
            }

            var icon = BitmapUtils.getBitmap(customIconStorage[action])
            icon = BitmapUtils.shrinkPreservingRatio(icon, targetIconSize, targetIconSize, true)

            val iconData = BitmapUtils.serialize(icon)
            val assetKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + index
            if (iconData != null) {
                putDataRequest.putAsset(assetKey, Asset.createFromBytes(iconData))
            }
        }

        putDataRequest.data = protoBuilder.build().toByteArray()

        dataClient.putDataItem(putDataRequest).await()
    }
}
