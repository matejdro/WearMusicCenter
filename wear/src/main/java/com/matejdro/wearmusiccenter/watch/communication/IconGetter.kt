package com.matejdro.wearmusiccenter.watch.communication

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataItem
import com.matejdro.wearmusiccenter.common.actions.StandardIcons
import com.matejdro.wearutils.messages.DataUtils
import com.matejdro.wearutils.miscutils.BitmapUtils

object IconGetter {
    fun getIcon(googleApiClient: GoogleApiClient, dataItem: DataItem, iconKey: String, actionKey: String): Drawable? {
        val resources = googleApiClient.context.resources

        if (dataItem.assets.containsKey(iconKey)) {
            // We have override custom icon. Lets use that
            val iconAsset = dataItem.assets[iconKey]
            val iconBitmap = BitmapUtils.deserialize(DataUtils.getByteArrayAsset(iconAsset,
                    googleApiClient))

            return BitmapDrawable(resources, iconBitmap)
        }

        // We did not receive icon via bluetooth. Check if it is one of the standard icons
        // that we have pre-stored on the watch
        val iconRes = StandardIcons.getIcon(actionKey)
        if (iconRes == 0) {
            return null
        }

        return resources.getDrawable(iconRes, null)
    }
}