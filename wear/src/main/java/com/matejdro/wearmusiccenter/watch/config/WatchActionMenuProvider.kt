package com.matejdro.wearmusiccenter.watch.config

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataItem
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.proto.WatchList
import com.matejdro.wearutils.messages.DataUtils
import com.matejdro.wearutils.miscutils.BitmapUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class WatchActionMenuProvider(private val googleApiClient: GoogleApiClient, rawData: LiveData<DataItem>) {
    val config = MutableLiveData<List<ButtonAction>>()

    private val dataObserver = Observer<DataItem> {
        if (it == null) {
            return@Observer
        }

        val dataItem = it

        launch(CommonPool) {
            val listProto = WatchList.parseFrom(it.data)

            val actions = listProto.actionsList.withIndex().map {
                val iconKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + it.index
                val iconAsset = dataItem.assets[iconKey]
                val icon = BitmapUtils.deserialize(DataUtils.getByteArrayAsset(iconAsset,
                        googleApiClient))

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