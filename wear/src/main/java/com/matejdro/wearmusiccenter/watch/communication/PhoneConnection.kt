package com.matejdro.wearmusiccenter.watch.communication

import android.arch.lifecycle.MutableLiveData
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
import com.matejdro.wearutils.lifecycle.ListenableLiveData
import com.matejdro.wearutils.lifecycle.LiveDataLifecycleCombiner
import com.matejdro.wearutils.lifecycle.LiveDataLifecycleListener
import com.matejdro.wearutils.lifecycle.Resource
import com.matejdro.wearutils.messages.DataUtils
import com.matejdro.wearutils.messages.MessagingUtils
import com.matejdro.wearutils.miscutils.BitmapUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

class PhoneConnection(private val context: Context) : DataApi.DataListener, LiveDataLifecycleListener {

    companion object {
        const val MESSAGE_CLOSE_CONNECTION = 0
        const val CONNECTION_CLOSE_DELAY_MS = 15_000L
    }

    val musicState = ListenableLiveData<Resource<MusicState>>()
    val albumArt = ListenableLiveData<Bitmap?>()

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
                val errorText = context.getString(R.string.error_play_services)
                musicState.postValue(Resource.error(errorText, null))

                GoogleApiAvailability.getInstance().showErrorNotification(context, result)
                return@post
            }

            Wearable.DataApi.addListener(googleApiClient, this)

            val phoneNode = MessagingUtils.getOtherNodeId(googleApiClient)
            if (phoneNode != null) {
                Wearable.MessageApi.sendMessage(googleApiClient, phoneNode, CommPaths.MESSAGE_WATCH_OPENED, null).await()
            } else {
                musicState.postValue(Resource.error(context.getString(R.string.no_phone), null))
            }

            loadCurrentActionConfig(CommPaths.DATA_PLAYING_ACTION_CONFIG, rawPlaybackConfig)
            loadCurrentActionConfig(CommPaths.DATA_STOPPING_ACTION_CONFIG, rawStoppedConfig)
            loadCurrentActionConfig(CommPaths.DATA_LIST_ITEMS, rawActionMenuConfig)
        }

        running = true
        musicState.value = Resource.loading(null)

        Timber.d("Running")
    }

    fun stop() {
        if (!running) {
            return
        }

        connectionHandler.post {
            Wearable.DataApi.removeListener(googleApiClient, this)

            val phoneNode = MessagingUtils.getOtherNodeId(googleApiClient)
            if (phoneNode != null) {
                Wearable.MessageApi.sendMessage(googleApiClient, phoneNode, CommPaths.MESSAGE_WATCH_CLOSED, null).await()
            }

            googleApiClient.disconnect()
        }

        running = false
    }

    fun close() {
        stop()
        connectionThread.quitSafely()
    }

    fun sendVolume(newVolume: Float) {
        launch(UI) {
            nextVolume = -1f

            if (sendingVolume) {
                nextVolume = newVolume
                return@launch
            }

            sendingVolume = true

            launch(CommonPool) {
                val phoneNode = MessagingUtils.getOtherNodeId(googleApiClient)
                if (phoneNode != null) {
                    Wearable.MessageApi.sendMessage(googleApiClient,
                            phoneNode,
                            CommPaths.MESSAGE_CHANGE_VOLUME,
                            FloatPacker.packFloat(newVolume)).await()
                }
            }.join()

            sendingVolume = false
            if (nextVolume >= 0) {
                sendVolume(nextVolume)
            }
        }
    }

    fun executeButtonAction(buttonInfo: ButtonInfo) {
        connectionHandler.post {
            val phoneNode = MessagingUtils.getOtherNodeId(googleApiClient)
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
            val phoneNode = MessagingUtils.getOtherNodeId(googleApiClient)
            if (phoneNode != null) {
                Wearable.MessageApi.sendMessage(googleApiClient,
                        phoneNode,
                        CommPaths.MESSAGE_EXECUTE_MENU_ACTION,
                        ByteBuffer.allocate(4).putInt(index).array()).await()
            }

        }
    }



    override fun onDataChanged(data: DataEventBuffer?) {
        if (data == null) {
            return
        }

        data.filter { it.type == DataEvent.TYPE_CHANGED }
                .map { it.dataItem }
                .forEach {
                    if (it.uri.path == CommPaths.DATA_MUSIC_STATE) {
                        val dataItem = it.freeze()

                        connectionHandler.post {
                            val receivedMusicState = MusicState.parseFrom(dataItem.data)

                            if (receivedMusicState.error) {
                                musicState.postValue(Resource.error(receivedMusicState.title, null))
                            } else {
                                musicState.postValue(Resource.success(receivedMusicState))

                                val phoneNode = MessagingUtils.getOtherNodeId(googleApiClient)
                                if (phoneNode != null) {
                                    Wearable.MessageApi.sendMessage(googleApiClient, phoneNode, CommPaths.MESSAGE_ACK, null)
                                }

                                val albumArtData = DataUtils.getByteArrayAsset(dataItem.assets[CommPaths.ASSET_ALBUM_ART],
                                        googleApiClient)
                                albumArt.postValue(BitmapUtils.deserialize(albumArtData))
                            }
                        }
                    } else if (it.uri.path == CommPaths.DATA_PLAYING_ACTION_CONFIG) {
                        rawPlaybackConfig.postValue(it.freeze())
                    } else if (it.uri.path == CommPaths.DATA_STOPPING_ACTION_CONFIG) {
                        rawStoppedConfig.postValue(it.freeze())
                    } else if (it.uri.path == CommPaths.DATA_LIST_ITEMS) {
                        rawActionMenuConfig.postValue(it.freeze())
                    }
                }

        data.release()
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