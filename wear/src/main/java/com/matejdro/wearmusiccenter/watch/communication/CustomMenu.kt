package com.matejdro.wearmusiccenter.watch.communication

import android.graphics.Bitmap
import com.matejdro.wearmusiccenter.proto.CustomList

data class CustomListWithBitmaps(
        val listTimestamp: Long,
        val listId: String,
        val items: List<CustomListItemWithIcon>
)

data class CustomListItemWithIcon(
        val listItem: CustomList.ListEntry,
        val icon: Bitmap?
)