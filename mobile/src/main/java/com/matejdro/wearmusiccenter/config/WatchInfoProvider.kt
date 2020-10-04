package com.matejdro.wearmusiccenter.config

import androidx.lifecycle.LiveData
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.*
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.proto.WatchInfo
import com.matejdro.wearutils.messages.DataUtils
import com.matejdro.wearutils.miscutils.BitmapUtils
import dagger.Reusable
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@Reusable
class WatchInfoProvider @Inject constructor(private val context: Context) : LiveData<WatchInfoWithIcons>(),
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    init {
        this.value = null
    }

    private val googleApiClient: GoogleApiClient = GoogleApiClient.Builder(context)
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

    private fun parseDataItem(dataItem: DataItem?) {
        if (dataItem == null) {
            this.value = null
            return
        }

        val watchInfo = WatchInfo.parseFrom(dataItem.data)
        val icons = ArrayList<Drawable?>()

        val frozenDataItem = dataItem.freeze()
        GlobalScope.launch(Dispatchers.Default) {
            for (buttonIndex in 0 until watchInfo.buttonsCount) {
                val buttonAssetName = CommPaths.ASSET_WATCH_INFO_BUTTON_PREFIX + "/" + buttonIndex
                val asset = frozenDataItem.assets[buttonAssetName]
                if (asset == null) {
                    icons.add(null)
                    continue
                }

                val iconBytes = DataUtils.getByteArrayAsset(asset, googleApiClient)
                val icon = BitmapUtils.deserialize(iconBytes)
                icons.add(BitmapDrawable(this@WatchInfoProvider.context.resources, icon))
            }

            postValue(WatchInfoWithIcons(watchInfo, icons))
        }
    }

    private fun retrieveCurrentValue() {
        Wearable.DataApi.getDataItems(googleApiClient,
                Uri.parse("wear://*" + CommPaths.DATA_WATCH_INFO))
                .setResultCallback {
                    val latestWatchData = it.maxBy { WatchInfo.parseFrom(it.data).time }
                    parseDataItem(latestWatchData)
                    it.release()
                }
    }

    private fun registerListener() {
        Wearable.DataApi.addListener(googleApiClient,
                this,
                Uri.parse("wear://*" + CommPaths.DATA_WATCH_INFO),
                DataApi.FILTER_LITERAL)
    }

    override fun onDataChanged(dataBuffer: DataEventBuffer) {
        dataBuffer
                .filter { it.type == DataEvent.TYPE_CHANGED }
                .map { it.dataItem }
                .firstOrNull()?.also(this::parseDataItem)

        dataBuffer.release()
    }

    override fun onConnected(p0: Bundle?) {
        retrieveCurrentValue()
        registerListener()
    }

    override fun onConnectionSuspended(p0: Int) = Unit

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        GoogleApiAvailability.getInstance().showErrorNotification(context, connectionResult.errorCode)
    }

    override fun onInactive() {
        if (googleApiClient.isConnected) {
            Wearable.DataApi.removeListener(googleApiClient, this)
        }

        googleApiClient.disconnect()
    }

    override fun onActive() {
        googleApiClient.connect()
    }
}
