package com.matejdro.wearmusiccenter.watch.communication

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.wearable.*
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.util.FloatPacker
import com.matejdro.wearmusiccenter.proto.CustomList
import com.matejdro.wearmusiccenter.proto.CustomListItemAction
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.proto.Notification
import com.matejdro.wearmusiccenter.watch.util.launchWithErrorHandling
import com.matejdro.wearutils.coroutines.await
import com.matejdro.wearutils.lifecycle.*
import com.matejdro.wearutils.messages.*
import com.matejdro.wearutils.miscutils.BitmapUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneConnection @Inject constructor(@ApplicationContext private val context: Context) : DataClient.OnDataChangedListener,
        CapabilityClient.OnCapabilityChangedListener,
        LiveDataLifecycleListener {

    private var scope: CoroutineScope? = null

    companion object {
        const val MESSAGE_CLOSE_CONNECTION = 0
        const val CONNECTION_CLOSE_DELAY_MS = 15_000L
    }

    val musicState = ListenableLiveData<Resource<MusicState>>()
    val albumArt = ListenableLiveData<Bitmap?>()
    val customList = ListenableLiveData<CustomListWithBitmaps>()

    val notification = SingleLiveEvent<com.matejdro.wearmusiccenter.watch.model.Notification>()

    val rawPlaybackConfig = MutableLiveData<DataItem>()
    val rawStoppedConfig = MutableLiveData<DataItem>()
    val rawActionMenuConfig = MutableLiveData<DataItem>()

    private val lifecycleObserver = LiveDataLifecycleCombiner(this)

    private val messageClient = Wearable.getMessageClient(context)
    private val dataClient = Wearable.getDataClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val closeHandler = ConnectionCloseHandler(WeakReference(this))

    private var sendingVolume = false
    private var nextVolume = -1f

    private var running = AtomicBoolean(false)

    init {
        lifecycleObserver.addLiveData(musicState)
        lifecycleObserver.addLiveData(albumArt)
    }

    private fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }

        scope = CoroutineScope(Job() + Dispatchers.Main)

        scope?.launchWithErrorHandling(context, musicState) {
            val capabilities = capabilityClient.getCapability(
                    CommPaths.PHONE_APP_CAPABILITY,
                    CapabilityClient.FILTER_REACHABLE
            ).await()

            onWatchConnectionUpdated(capabilities)

            dataClient.addListener(this)
            capabilityClient.addListener(this, CommPaths.PHONE_APP_CAPABILITY)

            loadCurrentActionConfig(CommPaths.DATA_PLAYING_ACTION_CONFIG, rawPlaybackConfig)
            loadCurrentActionConfig(CommPaths.DATA_STOPPING_ACTION_CONFIG, rawStoppedConfig)
            loadCurrentActionConfig(CommPaths.DATA_LIST_ITEMS, rawActionMenuConfig)

            musicState.postValue(Resource.loading(null))
        }
    }

    private fun stop() {
        if (!running.compareAndSet(true, false)) {
            return
        }

        scope?.launchWithErrorHandling(context, musicState) {
            try {
                dataClient.removeListener(this)

                val phoneNode = nodeClient.getNearestNodeId()
                if (phoneNode != null) {
                    messageClient.sendMessage(phoneNode, CommPaths.MESSAGE_WATCH_CLOSED, null).await()
                }
            } finally {
                scope?.cancel()
            }

        }
    }

    private fun onWatchConnectionUpdated(capabilityInfo: CapabilityInfo) {
        val firstNode = capabilityInfo.nodes.firstOrNull { it.isNearby }

        if (firstNode != null) {
            scope?.launchWithErrorHandling(context, musicState) {
                messageClient.sendMessage(capabilityInfo.nodes.first().id, CommPaths.MESSAGE_WATCH_OPENED, null).await()
            }
        } else {
            musicState.postValue(Resource.error(context.getString(R.string.no_phone), null))
        }
    }

    suspend fun sendManualCloseMessage() {
        if (!running.get()) {
            return
        }

        // Activity closes when manual close happens, so we must ignore cancel signal here
        withContext(NonCancellable) {
            val phoneNode = nodeClient.getNearestNodeId()
            if (phoneNode != null) {
                messageClient.sendMessage(phoneNode, CommPaths.MESSAGE_WATCH_CLOSED_MANUALLY, null).await()
            }
        }
    }

    fun sendVolume(newVolume: Float) {
        scope?.launchWithErrorHandling(context, musicState) {
            nextVolume = -1f

            if (sendingVolume) {
                nextVolume = newVolume
                return@launchWithErrorHandling
            }

            sendingVolume = true

            messageClient.sendMessageToNearestClient(
                    nodeClient,
                    CommPaths.MESSAGE_CHANGE_VOLUME,
                    FloatPacker.packFloat(newVolume)
            )
        }

        sendingVolume = false
        if (nextVolume >= 0) {
            sendVolume(nextVolume)
        }
    }

    suspend fun executeButtonAction(buttonInfo: ButtonInfo) {
        messageClient.sendMessageToNearestClient(
                nodeClient,
                CommPaths.MESSAGE_EXECUTE_ACTION,
                buttonInfo.buildProtoVersion().build().toByteArray()
        )
    }

    suspend fun executeMenuAction(index: Int) {
        messageClient.sendMessageToNearestClient(
                nodeClient,
                CommPaths.MESSAGE_EXECUTE_MENU_ACTION,
                ByteBuffer.allocate(4).putInt(index).array()
        )
    }

    suspend fun executeCustomMenuAction(listId: String, entryId: String) {
        messageClient.sendMessageToNearestClient(
                nodeClient,
                CommPaths.MESSAGE_CUSTOM_LIST_ITEM_SELECTED,
                CustomListItemAction.newBuilder()
                        .setListId(listId)
                        .setEntryId(entryId)
                        .build()
                        .toByteArray()
        )

    }


    private suspend fun sendAck() {
        messageClient.sendMessageToNearestClient(nodeClient, CommPaths.MESSAGE_ACK)
    }

    override fun onDataChanged(data: DataEventBuffer?) {
        if (data == null) {
            return
        }

        val frozenData = data.use { _ ->
            data.map { it.freeze() }
        }

        scope?.launchWithErrorHandling(context, musicState) {
            frozenData.filter { it.type == DataEvent.TYPE_CHANGED }
                    .map { it.dataItem }
                    .forEach {
                        when (it.uri.path) {
                            CommPaths.DATA_MUSIC_STATE -> {
                                val dataItem = it.freeze()

                                val receivedMusicState = MusicState.parseFrom(dataItem.data)

                                if (receivedMusicState.error) {
                                    musicState.postValue(Resource.error(receivedMusicState.title, null))
                                } else {
                                    musicState.postValue(Resource.success(receivedMusicState))

                                    sendAck()

                                    val albumArtData = dataItem.assets[CommPaths.ASSET_ALBUM_ART]
                                            ?.let { asset -> dataClient.getByteArrayAsset(asset) }
                                    albumArt.postValue(BitmapUtils.deserialize(albumArtData))
                                }
                            }
                            CommPaths.DATA_NOTIFICATION -> {
                                val dataItem = it.freeze()
                                val receivedNotification = Notification.parseFrom(dataItem.data)

                                sendAck()

                                val pictureData = dataItem.assets[CommPaths.ASSET_NOTIFICATION_BACKGROUND]
                                        ?.let { asset -> dataClient.getByteArrayAsset(asset) }
                                val picture = BitmapUtils.deserialize(pictureData)

                                val mergedNotification = com.matejdro.wearmusiccenter.watch.model.Notification(
                                        receivedNotification.title,
                                        receivedNotification.description,
                                        picture
                                )

                                notification.postValue(mergedNotification)
                            }
                            CommPaths.DATA_PLAYING_ACTION_CONFIG -> rawPlaybackConfig.postValue(it.freeze())
                            CommPaths.DATA_STOPPING_ACTION_CONFIG -> rawStoppedConfig.postValue(it.freeze())
                            CommPaths.DATA_LIST_ITEMS -> rawActionMenuConfig.postValue(it.freeze())
                            CommPaths.DATA_CUSTOM_LIST -> {
                                val dataItem = it.freeze()
                                val receivedCustomList = CustomList.parseFrom(dataItem.data)

                                val listItems = receivedCustomList.actionsList
                                        .mapIndexed { index, rawListEntry ->
                                            val pictureData = dataItem.assets[index.toString()]
                                                    ?.let { asset -> dataClient.getByteArrayAsset(asset) }

                                            val picture = BitmapUtils.deserialize(pictureData)

                                            CustomListItemWithIcon(
                                                    rawListEntry,
                                                    picture
                                            )
                                        }


                                customList.postValue(
                                        CustomListWithBitmaps(
                                                receivedCustomList.listTimestamp,
                                                receivedCustomList.listId,
                                                listItems
                                        )
                                )
                            }
                        }
                    }
        }
    }

    override fun onCapabilityChanged(capability: CapabilityInfo) {
        onWatchConnectionUpdated(capability)
    }

    private suspend fun loadCurrentActionConfig(configPath: String, targetLiveData: MutableLiveData<DataItem>) {
        val dataItems = dataClient.getDataItems(
                Uri.parse("wear://*$configPath"),
                DataClient.FILTER_LITERAL)
                .await()

        val dataItem = dataItems.firstOrNull() ?: return

        targetLiveData.postValue(dataItem.freeze())
        dataItems.release()
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

    suspend fun openPlaybackQueue() {
        messageClient.sendMessageToNearestClient(nodeClient, CommPaths.MESSAGE_OPEN_PLAYBACK_QUEUE)
    }

    private class ConnectionCloseHandler(val phoneConnection: WeakReference<PhoneConnection>) : Handler(Looper.getMainLooper()) {
        override fun dispatchMessage(msg: android.os.Message) {
            if (msg.what == MESSAGE_CLOSE_CONNECTION) {
                phoneConnection.get()?.stop()
            }
        }
    }
}