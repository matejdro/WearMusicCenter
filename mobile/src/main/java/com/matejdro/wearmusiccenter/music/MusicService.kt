package com.matejdro.wearmusiccenter.music

import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
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
import javax.inject.Inject

class MusicService : LifecycleService(), MessageApi.MessageListener {
    companion object {
        const val MESSAGE_STOP_SELF = 0
        const val ACK_TIMEOUT = 3000L
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

        //TODO fix notification and add remove event
        val persistentNotification = NotificationCompat.Builder(this)
                .setContentText("Music Service active")
                .setContentTitle("WearMusicCenter")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()

        startForeground(1, persistentNotification)

        Timber.d("Service started")
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

    private fun buildMusicStateAndTransmit(mediaController: MediaController?) {
        val musicStateBuilder = MusicState.newBuilder()
        var albumArt: Bitmap? = null

        musicStateBuilder.time = System.currentTimeMillis().toInt()

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
        if (musicState.equalsIgnoringTime(previousMusicState)) {
            return
        }

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

            Timber.d("AlbumArtSize " + albumArt?.width + " " + albumArt?.height + " " + watchInfo?.displayWidth + " " + watchInfo?.displayHeight)

            if (albumArt != null) {
                val albumArtAsset = Asset.createFromBytes(BitmapUtils.serialize(albumArt))
                putDataRequest.putAsset(CommPaths.ASSET_ALBUM_ART, albumArtAsset)
            }

            putDataRequest.data = musicState.toByteArray()
            putDataRequest.setUrgent()

            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).await()
            Timber.d("Transmitting " + musicState)
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
        } else if (event.path == CommPaths.MESSAGE_WATCH_OPENED) {
            buildMusicStateAndTransmit(currentMediaController)
        }
    }


    private class AckTimeoutHandler(val service: WeakReference<MusicService>) : android.os
    .Handler() {
        override fun dispatchMessage(msg: android.os.Message?) {
            if (msg?.what == MESSAGE_STOP_SELF) {
                service.get()?.stopSelf()
            }
        }
    }

    fun MusicState.equalsIgnoringTime(other: MusicState?): Boolean {
        return other != null &&
                other.playing == playing &&
                other.artist == artist &&
                other.title == title
    }

}