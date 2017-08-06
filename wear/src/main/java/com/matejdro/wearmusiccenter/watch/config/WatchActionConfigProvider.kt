package com.matejdro.wearmusiccenter.watch.config

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.support.v4.util.SimpleArrayMap
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataItem
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.proto.WatchActions
import com.matejdro.wearmusiccenter.watch.communication.IconGetter
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class WatchActionConfigProvider(private val googleApiClient: GoogleApiClient, rawConfigData: LiveData<DataItem>) {
    val updateListener = MutableLiveData<WatchActionConfigProvider>()

    var configMap = SimpleArrayMap<ButtonInfo, ButtonAction>()

    var volumeStep = 0.1f

    fun getAction(buttonInfo: ButtonInfo) : ButtonAction? {
        return configMap.get(buttonInfo)
    }

    fun isActionActive(buttonInfo: ButtonInfo) : Boolean {
        return configMap.containsKey(buttonInfo)
    }

    private val rawConfigObserver = Observer<DataItem> {
        configMap.clear()

        if (it == null) {
            return@Observer
        }

        val dataItem = it

        launch(UI) {
            val newConfigMap = SimpleArrayMap<ButtonInfo, ButtonAction>()

            launch(CommonPool) {
                val actions = WatchActions.parseFrom(it.data)
                volumeStep = actions.volumeStep

                for (action in actions.actionsList) {
                    val buttonInfo = ButtonInfo(action)

                    val iconKey = CommPaths.ASSET_BUTTON_ICON_PREFIX + buttonInfo.getKey()
                    val key = action.actionKey

                    val icon = IconGetter.getIcon(googleApiClient,
                            dataItem,
                            iconKey,
                            key)

                    newConfigMap.put(buttonInfo, ButtonAction(key, icon))
                }
            }.join()

            configMap = newConfigMap
            updateListener.value = this@WatchActionConfigProvider
        }
    }

    init {
        rawConfigData.observeForever(rawConfigObserver)
        updateListener.value = this
    }
}