package com.matejdro.wearmusiccenter.watch.config

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.collection.SimpleArrayMap
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataItem
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.proto.WatchActions
import com.matejdro.wearmusiccenter.watch.communication.IconGetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchActionConfigProvider(private val googleApiClient: GoogleApiClient, rawConfigData: LiveData<DataItem>) {
    val updateListener = MutableLiveData<WatchActionConfigProvider>()

    var configMap =
        SimpleArrayMap<ButtonInfo, ButtonAction>()

    var volumeStep = 0.1f

    fun getAction(buttonInfo: ButtonInfo) : ButtonAction? {
        return (configMap.get(buttonInfo) ?: configMap.get(buttonInfo.getLegacyButtonInfo()))
    }

    fun isActionActive(buttonInfo: ButtonInfo) : Boolean {
        return configMap.containsKey(buttonInfo) || configMap.containsKey(buttonInfo.getLegacyButtonInfo())
    }

    private val rawConfigObserver = Observer<DataItem> {
        configMap.clear()

        if (it == null) {
            return@Observer
        }

        val dataItem = it

        GlobalScope.launch(Dispatchers.Main) {
            val newConfigMap =
                SimpleArrayMap<ButtonInfo, ButtonAction>()

            withContext(Dispatchers.Default) {
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
