package com.matejdro.wearmusiccenter.music

import android.app.PendingIntent
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.NotificationCompat
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.*
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.WearMusicCenter
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.util.FloatPacker
import com.matejdro.wearmusiccenter.config.ActionConfigProvider
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.config.WatchInfoWithIcons
import com.matejdro.wearmusiccenter.di.GlobalConfig
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.proto.WatchActions
import com.matejdro.wearutils.miscutils.BitmapUtils
import timber.log.Timber
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import javax.inject.Inject

class MusicService : LifecycleService(), MessageApi.MessageListener {
    companion object {
        private const val MESSAGE_STOP_SELF = 0
        private const val ACK_TIMEOUT_MS = 10_000L

        private const val STOP_SELF_PENDING_INTENT_REQUEST_CODE = 333
        private const val ACTION_STOP_SELF = "STOP_SELF"
    }

    private lateinit var googleApiClient: GoogleApiClient
    private val connectionThread: HandlerThread = HandlerThread("Phone Connection")
    private lateinit var connectionHandler: Handler

    @Inject
    lateinit var mediaSessionProvider: ActiveMediaSessionProvider

    @Inject
    @field:GlobalConfig
    lateinit var configProvider: ActionConfigProvider

    @Inject
    lateinit var watchInfoProvider: WatchInfoProvider

    private var ackTimeoutHandler = AckTimeoutHandler(WeakReference(this))

    private var previousMusicState: MusicState? = null
    var currentMediaController: MediaController? = null
    private var firstMessage = true

    override fun onCreate() {
        super.onCreate()

        WearMusicCenter.getAppComponent().inject(this)

        connectionThread.start()
        connectionHandler = Handler(connectionThread.looper)

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build()

        connectionHandler.post {
            //TODO check for errors
            googleApiClient.blockingConnect()

            Wearable.MessageApi.addListener(googleApiClient, this, Uri.parse(CommPaths.MESSAGES_PREFIX), MessageApi
                    .FILTER_PREFIX)
        }

        mediaSessionProvider = ActiveMediaSessionProvider(this)
        mediaSessionProvider.observe(this, mediaCallback)

        watchInfoProvider.observe(this, Observer<WatchInfoWithIcons> {})

        val stopSelfIntent = Intent(this, MusicService::class.java)
        stopSelfIntent.action = ACTION_STOP_SELF

        val stopSelfPendingIntent = PendingIntent.getService(this,
                STOP_SELF_PENDING_INTENT_REQUEST_CODE,
                stopSelfIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        //TODO fix notification icon and text
        val persistentNotification = NotificationCompat.Builder(this)
                .setContentText("Music Service active")
                .setContentTitle("WearMusicCenter")
                .setContentIntent(stopSelfPendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()

        startForeground(1, persistentNotification)

        Timber.d("Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SELF) {
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.d("Service stopped")

        connectionHandler.post {
            Wearable.MessageApi.removeListener(googleApiClient, this)
            googleApiClient.disconnect()
        }
        connectionThread.quitSafely()


        super.onDestroy()
    }

    val mediaCallback = android.arch.lifecycle.Observer<MediaController?> {
        buildMusicStateAndTransmit(it)

        if (it != null) {
            currentMediaController = it
        }
    }

    private fun updateVolume(newVolume: Float) {
        val previousMediaController = currentMediaController ?: return

        val maxVolume = previousMediaController.playbackInfo.maxVolume
        previousMediaController.setVolumeTo((maxVolume * newVolume).toInt(), 0)
    }

    private fun executeAction(buttonInfo: ButtonInfo) {
        val playing = currentMediaController?.isPlaying() ?: false

        val config = if (playing)
            configProvider.getPlayingConfig()
        else
            configProvider.getStoppedConfig()

        val buttonAction = config.getScreenAction(buttonInfo) ?: return
        buttonAction.execute(this)
    }

    private fun executeMenuAction(index: Int) {
        val config = configProvider.getActionList()
        val list = config.actions

        if (index < 0 || index >= list.size) {
            Timber.e("Action out of bounds: %d", index)
            return
        }

        list[index].execute(this)
    }

    private fun buildMusicStateAndTransmit(mediaController: MediaController?) {
        val musicStateBuilder = MusicState.newBuilder()
        var albumArt: Bitmap? = null

        if (firstMessage) {
            // Add time to the first message to make sure it gets transmitted even if it is
            // identical to the previous one
            musicStateBuilder.time = System.currentTimeMillis().toInt()
            firstMessage = false
        }

        if (mediaController == null) {
            musicStateBuilder.playing = false
        } else {
            musicStateBuilder.playing = mediaController.playbackState?.isPlaying() == true

            val meta = mediaController.metadata
            if (meta != null) {
                musicStateBuilder.artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                musicStateBuilder.title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
                albumArt = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            }

            val volume = mediaController.playbackInfo.currentVolume.toFloat() / mediaController.playbackInfo.maxVolume
            musicStateBuilder.volume = volume
        }


        val musicState = musicStateBuilder.build()

        Timber.d("TransmittingToWear")
        transmitToWear(musicState, albumArt)
    }

    private fun transmitToWear(musicState: MusicState, originalAlbumArt: Bitmap?) {
        connectionHandler.post {
            val putDataRequest = PutDataRequest.create(CommPaths.DATA_MUSIC_STATE)

            var albumArt = originalAlbumArt
            val watchInfo = watchInfoProvider.value?.watchInfo
            if (watchInfo != null && albumArt != null) {
                albumArt = BitmapUtils.resizeAndCrop(albumArt,
                        watchInfo.displayWidth,
                        watchInfo.displayHeight,
                        true)
            }

            if (albumArt != null) {
                val albumArtAsset = Asset.createFromBytes(BitmapUtils.serialize(albumArt))
                putDataRequest.putAsset(CommPaths.ASSET_ALBUM_ART, albumArtAsset)
            }

            putDataRequest.data = musicState.toByteArray()
            putDataRequest.setUrgent()

            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).await()

            ackTimeoutHandler.sendEmptyMessageDelayed(0, ACK_TIMEOUT_MS)
        }
    }

    override fun onMessageReceived(event: MessageEvent?) {
        if (event == null) {
            return
        }

        if (event.path == CommPaths.MESSAGE_WATCH_CLOSED) {
            stopSelf()
        } else if (event.path == CommPaths.MESSAGE_ACK) {
            ackTimeoutHandler.removeMessages(MESSAGE_STOP_SELF)
        } else if (event.path == CommPaths.MESSAGE_CHANGE_VOLUME) {
            updateVolume(FloatPacker.unpackFloat(event.data))
        } else if (event.path == CommPaths.MESSAGE_EXECUTE_ACTION) {
            executeAction(ButtonInfo(WatchActions.ProtoButtonInfo.parseFrom(event.data)))
        } else if (event.path == CommPaths.MESSAGE_EXECUTE_MENU_ACTION) {
            executeMenuAction(ByteBuffer.wrap(event.data).int)
        } else if (event.path == CommPaths.MESSAGE_WATCH_OPENED) {
            buildMusicStateAndTransmit(currentMediaController)
        }
    }


    private class AckTimeoutHandler(val service: WeakReference<MusicService>) : android.os
    .Handler() {
        override fun dispatchMessage(msg: android.os.Message?) {
            if (msg?.what == MESSAGE_STOP_SELF) {
                Timber.d("TIMEOUT!")
                service.get()?.stopSelf()
            }
        }
    }
}