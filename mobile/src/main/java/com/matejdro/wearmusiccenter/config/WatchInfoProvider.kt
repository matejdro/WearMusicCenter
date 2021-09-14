package com.matejdro.wearmusiccenter.config

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.lifecycle.LiveData
import com.google.android.gms.wearable.*
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.proto.WatchInfo
import com.matejdro.wearmusiccenter.util.launchWithPlayServicesErrorHandling
import com.matejdro.wearutils.coroutines.await
import com.matejdro.wearutils.messages.getByteArrayAsset
import com.matejdro.wearutils.miscutils.BitmapUtils
import dagger.Reusable
import kotlinx.coroutines.*
import javax.inject.Inject

@Reusable
class WatchInfoProvider @Inject constructor(private val context: Context) :
        LiveData<WatchInfoWithIcons>(), DataClient.OnDataChangedListener {

    private val dataClient = Wearable.getDataClient(context)

    private var coroutineScope = CoroutineScope(Job())

    init {
        this.value = null
    }

    private fun parseDataItem(dataItem: DataItem?) {
        if (dataItem == null) {
            this.value = null
            return
        }

        val watchInfo = WatchInfo.parseFrom(dataItem.data)

        val frozenDataItem = dataItem.freeze()
        coroutineScope.launchWithPlayServicesErrorHandling(context) {
            val icons = frozenDataItem.assets.keys
                    .filter { it.startsWith(CommPaths.ASSET_WATCH_INFO_BUTTON_PREFIX) }
                    .mapNotNull { assetPrefix ->
                        val buttonCode = assetPrefix.removePrefix(CommPaths.ASSET_WATCH_INFO_BUTTON_PREFIX + "/").toIntOrNull()
                                ?: return@mapNotNull null

                        val buttonAssetName =
                                CommPaths.ASSET_WATCH_INFO_BUTTON_PREFIX + "/" + buttonCode

                        val asset = frozenDataItem.assets[buttonAssetName] ?: return@mapNotNull null

                        val iconBytes = dataClient.getByteArrayAsset(asset)
                        val icon = BitmapUtils.deserialize(iconBytes)

                        buttonCode to BitmapDrawable(this@WatchInfoProvider.context.resources, icon)
                    }
                    .toMap()

            postValue(WatchInfoWithIcons(watchInfo, icons))
        }
    }

    private fun retrieveCurrentValue() {
        coroutineScope.launchWithPlayServicesErrorHandling(context) {
            val items = dataClient.getDataItems(
                    Uri.parse("wear://*" + CommPaths.DATA_WATCH_INFO)
            ).await()

            val latestWatchData = items.maxByOrNull { WatchInfo.parseFrom(it.data).time }
            parseDataItem(latestWatchData)
            items.release()
        }
    }

    private fun registerListener() {
        dataClient.addListener(
                this,
                Uri.parse("wear://*" + CommPaths.DATA_WATCH_INFO),
                DataClient.FILTER_LITERAL
        )
    }

    override fun onDataChanged(dataBuffer: DataEventBuffer) {
        dataBuffer
                .filter { it.type == DataEvent.TYPE_CHANGED }
                .map { it.dataItem }
                .firstOrNull()?.also(this::parseDataItem)

        dataBuffer.release()
    }

    override fun onInactive() {
        coroutineScope.cancel()
        dataClient.removeListener(this)
    }

    override fun onActive() {
        coroutineScope = CoroutineScope(Job())

        retrieveCurrentValue()
        registerListener()
    }
}
