package com.matejdro.wearmusiccenter.watch.config

import android.content.Context
import androidx.collection.SimpleArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.proto.WatchActions
import com.matejdro.wearmusiccenter.watch.communication.getIcon
import kotlinx.coroutines.*

class WatchActionConfigProvider(context: Context, scope: CoroutineScope, rawConfigData: LiveData<DataItem>) {
    private val dataClient = Wearable.getDataClient(context)

    val updateListener = MutableLiveData<WatchActionConfigProvider>()

    var configMap =
            SimpleArrayMap<ButtonInfo, ButtonAction>()

    var volumeStep = 0.1f

    fun getAction(buttonInfo: ButtonInfo): ButtonAction? {
        return (configMap.get(buttonInfo) ?: configMap.get(buttonInfo.getLegacyButtonInfo()))
    }

    fun isActionActive(buttonInfo: ButtonInfo): Boolean {
        return configMap.containsKey(buttonInfo) || configMap.containsKey(buttonInfo.getLegacyButtonInfo())
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private val rawConfigObserver = Observer<DataItem> {
        configMap.clear()

        if (it == null) {
            return@Observer
        }

        val dataItem = it

        scope.launch {
            val newConfigMap =
                    SimpleArrayMap<ButtonInfo, ButtonAction>()

            val actions = WatchActions.parseFrom(it.data)
            volumeStep = actions.volumeStep

            for (action in actions.actionsList) {
                val buttonInfo = ButtonInfo(action)

                val iconKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + buttonInfo.getKey()
                val key = action.actionKey

                val icon = dataClient.getIcon(
                        dataItem,
                        iconKey,
                        key
                )

                newConfigMap.put(buttonInfo, ButtonAction(key, icon))
            }

            configMap = newConfigMap
            updateListener.value = this@WatchActionConfigProvider
        }
    }

    init {
        rawConfigData.observeForever(rawConfigObserver)
        updateListener.value = this
    }
}
