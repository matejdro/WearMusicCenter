package com.matejdro.wearmusiccenter.watch.config

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataItem
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.proto.WatchList
import com.matejdro.wearmusiccenter.watch.communication.IconGetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WatchActionMenuProvider(private val googleApiClient: GoogleApiClient, rawData: LiveData<DataItem>) {
    val config = MutableLiveData<List<ButtonAction>>()

    private val dataObserver = Observer<DataItem> { dataItem ->
        if (dataItem == null) {
            return@Observer
        }

        GlobalScope.launch(Dispatchers.Default) {
            val listProto = WatchList.parseFrom(dataItem.data)

            val actions = listProto.actionsList.withIndex().map {
                val iconKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + it.index
                val icon = IconGetter.getIcon(googleApiClient,
                        dataItem,
                        iconKey,
                        it.value.actionKey)

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
