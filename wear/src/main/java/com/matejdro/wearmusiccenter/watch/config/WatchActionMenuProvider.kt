package com.matejdro.wearmusiccenter.watch.config

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.proto.WatchList
import com.matejdro.wearmusiccenter.watch.communication.getIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WatchActionMenuProvider(context: Context, coroutineScope: CoroutineScope, rawData: LiveData<DataItem>) {
    private val dataClient = Wearable.getDataClient(context)
    val config = MutableLiveData<List<ButtonAction>>()

    private val dataObserver = Observer<DataItem?> { dataItem ->
        if (dataItem == null) {
            return@Observer
        }

        coroutineScope.launch {
            @Suppress("BlockingMethodInNonBlockingContext")
            val listProto = WatchList.parseFrom(dataItem.data)

            val actions = listProto.actionsList.withIndex().map {
                val iconKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + it.index
                val icon = dataClient.getIcon(
                        dataItem,
                        iconKey,
                        it.value.actionKey
                )

                ButtonAction(it.value.actionKey,
                        icon,
                        it.value.actionTitle)
            }.toList()

            config.postValue(actions)
        }
    }

    init {
        rawData.observeForever(dataObserver)
    }
}
