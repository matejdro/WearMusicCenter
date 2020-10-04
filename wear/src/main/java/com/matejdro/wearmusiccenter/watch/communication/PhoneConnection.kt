package com.matejdro.wearmusiccenter.watch.communication

import androidx.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.*
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.util.FloatPacker
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.proto.Notification
import com.matejdro.wearutils.lifecycle.*
import com.matejdro.wearutils.messages.DataUtils
import com.matejdro.wearutils.messages.getOtherNodeId
import com.matejdro.wearutils.miscutils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

class PhoneConnection(private val context: Context) : DataApi.DataListener, CapabilityApi.CapabilityListener, LiveDataLifecycleListener {

    companion object {
        const val MESSAGE_CLOSE_CONNECTION = 0
        const val CONNECTION_CLOSE_DELAY_MS = 15_000L
    }

    val musicState = ListenableLiveData<Resource<MusicState>>()
    val albumArt = ListenableLiveData<Bitmap?>()

    val notification = SingleLiveEvent<com.matejdro.wearmusiccenter.watch.model.Notification>()

    val rawPlaybackConfig = MutableLiveData<DataItem>()
    val rawStoppedConfig = MutableLiveData<DataItem>()
    val rawActionMenuConfig = MutableLiveData<DataItem>()

    private val lifecycleObserver = LiveDataLifecycleCombiner(this)

    val googleApiClient: GoogleApiClient
    private val connectionThread: HandlerThread = HandlerThread("Phone Connection")
    private val connectionHandler: Handler

    private val closeHandler = ConnectionCloseHandler(WeakReference(this))

    private var sendingVolume = false
    private var nextVolume = -1f

    var running = false

    init {
        connectionThread.start()
        connectionHandler = Handler(connectionThread.looper)

        googleApiClient = GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build()


        lifecycleObserver.addLiveData(musicState)
        lifecycleObserver.addLiveData(albumArt)
    }

    private fun start() {
        if (running) {
            return
        }

        connectionHandler.post {
            val result = googleApiClient.blockingConnect()

            if (!result.isSuccess) {
                val errorText = result.errorMessage ?: context.getString(R.string.error_play_services)
                musicState.postValue(Resource.error(errorText, null, result))

                Timber.e("Play Services error: %d %s", result.errorCode, result.errorMessage)
                return@post
            }

            val capabilities = Wearable.CapabilityApi.getCapability(googleApiClient,
                    CommPaths.PHONE_APP_CAPABILITY,
                    CapabilityApi.FILTER_REACHABLE)
                    .await()
                    .capability

            onWatchConnectionUpdated(capabilities)

            Wearable.DataApi.addListener(googleApiClient, this)

            Wearable.CapabilityApi.addCapabilityListener(googleApiClient, this, CommPaths.PHONE_APP_CAPABILITY)

            loadCurrentActionConfig(CommPaths.DATA_PLAYING_ACTION_CONFIG, rawPlaybackConfig)
            loadCurrentActionConfig(CommPaths.DATA_STOPPING_ACTION_CONFIG, rawStoppedConfig)
            loadCurrentActionConfig(CommPaths.DATA_LIST_ITEMS, rawActionMenuConfig)
        }

        running = true
        musicState.value = Resource.loading(null)
    }

    fun stop() {
        if (!running) {
            return
        }

        running = false

        connectionHandler.post {
            Wearable.DataApi.removeListener(googleApiClient, this)

            val phoneNode = getOtherNodeId(googleApiClient)
            if (phoneNode != null) {
                Wearable.MessageApi.sendMessage(googleApiClient, phoneNode, CommPaths.MESSAGE_WATCH_CLOSED, null).await()
            }

            googleApiClient.disconnect()
        }
    }

    private fun onWatchConnectionUpdated(capabilityInfo: CapabilityInfo) {
        val watchConnected = capabilityInfo.nodes.firstOrNull()?.isNearby == true

        if (watchConnected) {
            Wearable.MessageApi.sendMessage(googleApiClient, capabilityInfo.nodes.first()!!.id, CommPaths.MESSAGE_WATCH_OPENED, null).await()
        } else {
            musicState.postValue(Resource.error(context.getString(R.string.no_phone), null))
        }
    }

    fun sendManualCloseMessage() {
        if (!running) {
            return
        }

        connectionHandler.post {
            val phoneNode = getOtherNodeId(googleApiClient)
            if (phoneNode != null) {
                Wearable.MessageApi.sendMessage(googleApiClient, phoneNode, CommPaths.MESSAGE_WATCH_CLOSED_MANUALLY, null).await()
            }
        }
    }

    fun close() {
        stop()
        connectionThread.quitSafely()
    }

    fun sendVolume(newVolume: Float) {
        GlobalScope.launch(Dispatchers.Main) {
            nextVolume = -1f

            if (sendingVolume) {
                nextVolume = newVolume
                return@launch
            }

            sendingVolume = true

            withContext(Dispatchers.Default) {
                val phoneNode = getOtherNodeId(googleApiClient)
                if (phoneNode != null) {
                    Wearable.MessageApi.sendMessage(googleApiClient,
                            phoneNode,
                            CommPaths.MESSAGE_CHANGE_VOLUME,
                            FloatPacker.packFloat(newVolume)).await()
                }
            }

            sendingVolume = false
            if (nextVolume >= 0) {
                sendVolume(nextVolume)
            }
        }
    }

    fun executeButtonAction(buttonInfo: ButtonInfo) {
        connectionHandler.post {
            val phoneNode = getOtherNodeId(googleApiClient)
            if (phoneNode != null) {
                Wearable.MessageApi.sendMessage(googleApiClient,
                        phoneNode,
                        CommPaths.MESSAGE_EXECUTE_ACTION,
                        buttonInfo.buildProtoVersion().build().toByteArray()).await()
            }

        }
    }

    fun executeMenuAction(index: Int) {
        connectionHandler.post {
            val phoneNode = getOtherNodeId(googleApiClient)
            if (phoneNode != null) {
                Wearable.MessageApi.sendMessage(googleApiClient,
                        phoneNode,
                        CommPaths.MESSAGE_EXECUTE_MENU_ACTION,
                        ByteBuffer.allocate(4).putInt(index).array()).await()
            }

        }
    }

    private fun sendAck() {
        val phoneNode = getOtherNodeId(googleApiClient)
        if (phoneNode != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, phoneNode, CommPaths.MESSAGE_ACK, null)
        }
    }


    override fun onDataChanged(data: DataEventBuffer?) {
        if (data == null) {
            return
        }

        data.filter { it.type == DataEvent.TYPE_CHANGED }
                .map { it.dataItem }
                .forEach {
                    when (it.uri.path) {
                        CommPaths.DATA_MUSIC_STATE -> {
                            val dataItem = it.freeze()

                            connectionHandler.post {
                                val receivedMusicState = MusicState.parseFrom(dataItem.data)

                                if (receivedMusicState.error) {
                                    musicState.postValue(Resource.error(receivedMusicState.title, null))
                                } else {
                                    musicState.postValue(Resource.success(receivedMusicState))

                                    sendAck()

                                    val albumArtData = DataUtils.getByteArrayAsset(dataItem.assets[CommPaths.ASSET_ALBUM_ART],
                                            googleApiClient)
                                    albumArt.postValue(BitmapUtils.deserialize(albumArtData))
                                }
                            }
                        }
                        CommPaths.DATA_NOTIFICATION -> {
                            val dataItem = it.freeze()
                            connectionHandler.post {
                                val receivedNotification = Notification.parseFrom(dataItem.data)

                                sendAck()

                                val pictureData = DataUtils.getByteArrayAsset(dataItem.assets[CommPaths.ASSET_NOTIFICATION_BACKGROUND],
                                        googleApiClient)
                                val picture = BitmapUtils.deserialize(pictureData)

                                val mergedNotification = com.matejdro.wearmusiccenter.watch.model.Notification(
                                        receivedNotification.title,
                                        receivedNotification.description,
                                        picture
                                )

                                notification.postValue(mergedNotification)
                            }
                        }
                        CommPaths.DATA_PLAYING_ACTION_CONFIG -> rawPlaybackConfig.postValue(it.freeze())
                        CommPaths.DATA_STOPPING_ACTION_CONFIG -> rawStoppedConfig.postValue(it.freeze())
                        CommPaths.DATA_LIST_ITEMS -> rawActionMenuConfig.postValue(it.freeze())
                    }
                }

        data.release()
    }

    override fun onCapabilityChanged(capability: CapabilityInfo) {
        connectionHandler.post { onWatchConnectionUpdated(capability) }
    }

    private fun loadCurrentActionConfig(configPath: String, targetLiveData: MutableLiveData<DataItem>) {
        val dataItems = Wearable.DataApi.getDataItems(googleApiClient,
                Uri.parse("wear://*" + configPath),
                DataApi.FILTER_LITERAL)

        val items = dataItems.await()

        val dataItem = items.firstOrNull() ?: return

        targetLiveData.postValue(dataItem.freeze())
        items.release()
    }

    override fun onInactive() {
        // Delay connection closing for a bit to make sure it is not just brief configuration change

        closeHandler.removeMessages(MESSAGE_CLOSE_CONNECTION)
        closeHandler.sendEmptyMessageDelayed(MESSAGE_CLOSE_CONNECTION, CONNECTION_CLOSE_DELAY_MS)
    }

    override fun onActive() {
        closeHandler.removeMessages(MESSAGE_CLOSE_CONNECTION)
        start()
    }

    private class ConnectionCloseHandler(val phoneConnection: java.lang.ref.WeakReference<PhoneConnection>) : android.os
    .Handler() {
        override fun dispatchMessage(msg: android.os.Message?) {
            if (msg?.what == MESSAGE_CLOSE_CONNECTION) {
                phoneConnection.get()?.stop()
            }
        }
    }

}
