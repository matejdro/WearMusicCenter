package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.CustomLists
import com.matejdro.wearmusiccenter.config.buttons.ConfigConstants
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.proto.CustomList
import com.matejdro.wearutils.miscutils.BitmapUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class OpenPlaylistAction : SelectableAction {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        GlobalScope.launch {
            // TODO take density of Watch's display into account
            val targetIconSize = (ConfigConstants.BUTTON_ICON_SIZE_DP)

            val playlist = service.currentMediaController?.queue?.take(20)
            val putDataRequest = PutDataRequest.create(CommPaths.DATA_CUSTOM_LIST)

            val protoList = if (playlist != null) {
                playlist.mapIndexed { index, queueItem ->
                    var icon = queueItem.description.iconBitmap
                            ?: queueItem.description.iconUri?.let {
                                BitmapUtils.getBitmap(BitmapUtils.getDrawableFromUri(service, it))
                            }

                    icon = BitmapUtils.shrinkPreservingRatio(
                            icon,
                            targetIconSize,
                            targetIconSize,
                            true
                    )


                    val iconData = BitmapUtils.serialize(icon)
                    if (iconData != null) {
                        putDataRequest.putAsset(index.toString(), Asset.createFromBytes(iconData))
                    } else {
                        putDataRequest.removeAsset(index.toString())
                    }

                    CustomList.ListEntry.newBuilder()
                            .setEntryId(queueItem.queueId.toString())
                            .setEntryTitle(queueItem.description.title?.toString() ?: "")
                            .build()
                }
            } else {
                putDataRequest.removeAsset("0")
                listOf(
                        CustomList.ListEntry.newBuilder()
                                .setEntryId(CustomLists.SPECIAL_ITEM_ERROR)
                                .setEntryTitle(service.getString(R.string.error_playlist_not_supported))
                                .build()
                )

            }

            val protoData = CustomList.newBuilder()
                    .addAllActions(protoList)
                    .setListId(CustomLists.PLAYLIST)
                    .setListTimestamp(System.currentTimeMillis())
                    .build()

            putDataRequest.data = protoData.toByteArray()

            Wearable.DataApi.putDataItem(service.googleApiClient, putDataRequest)
        }
    }

    override fun retrieveTitle(): String = context.getString(R.string.open_playlist_menu)
    override val defaultIcon: Drawable
        get() = context.getDrawable(R.drawable.action_open_playlist)!!
}
