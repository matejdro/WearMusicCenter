package com.matejdro.wearmusiccenter.music

import android.annotation.TargetApi
import android.app.*
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.*
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.WearMusicCenter
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.MiscPreferences
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.util.FloatPacker
import com.matejdro.wearmusiccenter.config.ActionConfigProvider
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.config.WatchInfoWithIcons
import com.matejdro.wearmusiccenter.di.GlobalConfig
import com.matejdro.wearmusiccenter.notifications.NotificationProvider
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.proto.WatchActions
import com.matejdro.wearutils.lifecycle.Resource
import com.matejdro.wearutils.miscutils.BitmapUtils
import com.matejdro.wearutils.preferences.definition.Preferences
import com.matejdro.wearvibrationcenter.notificationprovider.ReceivedNotification
import timber.log.Timber
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import javax.inject.Inject

class MusicService : LifecycleService(), MessageApi.MessageListener {
    companion object {
        const val ACTION_START_FROM_WATCH = "START_FROM_WATCH"
        const val ACTION_NOTIFICATION_SERVICE_ACTIVATED = "NOTIFICATION_SERVICE_ACTIVATED"

        private const val MESSAGE_STOP_SELF = 0
        private const val ACK_TIMEOUT_MS = 10_000L

        private const val STOP_SELF_PENDING_INTENT_REQUEST_CODE = 333
        private const val ACTION_STOP_SELF = "STOP_SELF"
        private const val KEY_NOTIFICATION_CHANNEL = "Service_Channel"
        private const val KEY_NOTIFICATION_CHANNEL_ERRORS = "Error notifications"

        private const val NOTIFICATION_ID_PERSISTENT = 1
        private const val NOTIFICATION_ID_SERVICE_ERROR = 2

        var active = false
            private set
    }

    private lateinit var googleApiClient: GoogleApiClient
    private val connectionThread: HandlerThread = HandlerThread("Phone Connection")
    private lateinit var connectionHandler: Handler

    private lateinit var preferences: SharedPreferences

    @Inject
    lateinit var mediaSessionProvider: ActiveMediaSessionProvider

    @Inject
    @field:GlobalConfig
    lateinit var configProvider: ActionConfigProvider

    @Inject
    lateinit var watchInfoProvider: WatchInfoProvider

    @Inject
    lateinit var notificationProvider: NotificationProvider

    private var ackTimeoutHandler = AckTimeoutHandler(WeakReference(this))

    private var previousMusicState: MusicState? = null
    var currentMediaController: MediaController? = null
    private var firstMessage = true

    private var startedFromWatch = false

    override fun onCreate() {
        super.onCreate()

        WearMusicCenter.getAppComponent().inject(this)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        connectionThread.start()
        connectionHandler = Handler(connectionThread.looper)

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build()

        connectionHandler.post {
            val connectionStatus = googleApiClient.blockingConnect()
            if (!connectionStatus.isSuccess) {
                GoogleApiAvailability.getInstance().showErrorNotification(this, connectionStatus)
                stopSelf()
                return@post
            }

            Wearable.MessageApi.addListener(googleApiClient, this, Uri.parse(CommPaths.MESSAGES_PREFIX), MessageApi
                    .FILTER_PREFIX)
        }

        mediaSessionProvider = ActiveMediaSessionProvider(this)
        mediaSessionProvider.observe(this, mediaCallback)

        watchInfoProvider.observe(this, Observer<WatchInfoWithIcons> {})

        if (Preferences.getBoolean(preferences, MiscPreferences.ENABLE_NOTIFICATION_POPUP)) {
            notificationProvider.observe(this, notificationCallback)
        }

        val stopSelfIntent = Intent(this, MusicService::class.java)
        stopSelfIntent.action = ACTION_STOP_SELF

        val stopSelfPendingIntent = PendingIntent.getService(this,
                STOP_SELF_PENDING_INTENT_REQUEST_CODE,
                stopSelfIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(this, KEY_NOTIFICATION_CHANNEL)
                .setContentTitle(getString(R.string.music_control_active))
                .setContentText(getString(R.string.tap_to_force_stop))
                .setContentIntent(stopSelfPendingIntent)
                .setSmallIcon(R.drawable.ic_notification)


        // This is still needed for Pre-O versions, so it must be used, even if it is deprecated.
        @Suppress("DEPRECATION")
        notificationBuilder.priority = Notification.PRIORITY_MIN

        startForeground(NOTIFICATION_ID_PERSISTENT, notificationBuilder.build())

        active = true
        Timber.d("Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if ((intent?.action) == ACTION_START_FROM_WATCH) {
            startedFromWatch = true
        } else if (intent?.action == ACTION_STOP_SELF || !startedFromWatch) {
            stopSelf()
            return Service.START_NOT_STICKY
        } else if (intent?.action == ACTION_NOTIFICATION_SERVICE_ACTIVATED) {
            mediaSessionProvider.activate()
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_SERVICE_ERROR)
        }

        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    override fun onDestroy() {
        Timber.d("Service stopped")

        connectionHandler.post {
            Wearable.MessageApi.removeListener(googleApiClient, this)
            googleApiClient.disconnect()
        }
        connectionThread.quitSafely()

        active = false

        super.onDestroy()
    }

    private val mediaCallback = Observer<Resource<MediaController>?> {
        when {
            it == null -> {
                buildMusicStateAndTransmit(null)
            }
            it.status == Resource.Status.ERROR -> {
                transmitError(it.message ?: "")

                if (it.message == getString(R.string.error_notification_access)) {
                    showNotificationServiceErrorNotification()
                }
            }
            else -> {
                currentMediaController = it.data
                buildMusicStateAndTransmit(currentMediaController)
            }
        }
    }

    private val notificationCallback = Observer<ReceivedNotification> {
        if (it == null) {
            return@Observer
        }

        connectionHandler.post {
            val putDataRequest = PutDataRequest.create(CommPaths.DATA_NOTIFICATION)

            val protoNotification = com.matejdro.wearmusiccenter.proto.Notification.newBuilder()
                    .setTitle(it.title.trim())
                    .setDescription(it.description.trim())
                    .setTime(System.currentTimeMillis().toInt())
                    .build()

            it.imageDataPng?.let {
                val albumArtAsset = Asset.createFromBytes(it)
                putDataRequest.putAsset(CommPaths.ASSET_NOTIFICATION_BACKGROUND, albumArtAsset)
            }

            putDataRequest.data = protoNotification.toByteArray()
            putDataRequest.setUrgent()

            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest)
            ackTimeoutHandler.sendEmptyMessageDelayed(MESSAGE_STOP_SELF, ACK_TIMEOUT_MS)
        }
    }

    private fun updateVolume(newVolume: Float) {
        val previousMediaController = currentMediaController ?: return

        val maxVolume = previousMediaController.playbackInfo.maxVolume
        previousMediaController.setVolumeTo((maxVolume * newVolume).toInt(), 0)
    }

    private fun executeAction(buttonInfo: ButtonInfo) {
        val playing = currentMediaController?.isPlaying() == true

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
                meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.let {
                    musicStateBuilder.artist = it
                }
                meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.let {
                    musicStateBuilder.title = it
                }

                albumArt = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            }

            val volume = mediaController.playbackInfo.currentVolume.toFloat() / mediaController.playbackInfo.maxVolume
            musicStateBuilder.volume = volume
        }


        val musicState = musicStateBuilder.build()

        Timber.d("TransmittingToWear %s", musicState)
        transmitToWear(musicState, albumArt)
    }

    private fun transmitToWear(musicState: MusicState, originalAlbumArt: Bitmap?) = connectionHandler.post {
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

            ackTimeoutHandler.sendEmptyMessageDelayed(MESSAGE_STOP_SELF, ACK_TIMEOUT_MS)
    }

    private fun transmitError(error: String) = connectionHandler.post {
        val musicStateBuilder = MusicState.newBuilder()

        if (firstMessage) {
            // Add time to the first message to make sure it gets transmitted even if it is
            // identical to the previous one
            musicStateBuilder.time = System.currentTimeMillis().toInt()
            firstMessage = false
        }

        musicStateBuilder.error = true
        musicStateBuilder.title = error
        musicStateBuilder.playing = false

        val musicState = musicStateBuilder.build()

        val putDataRequest = PutDataRequest.create(CommPaths.DATA_MUSIC_STATE)

        putDataRequest.data = musicState.toByteArray()
        putDataRequest.setUrgent()

        Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).await()
    }

    private fun showNotificationServiceErrorNotification() {
        val notificationManagerIntent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")

        val notificationManagerPendingIntent = PendingIntent.getActivity(this,
                STOP_SELF_PENDING_INTENT_REQUEST_CODE,
                notificationManagerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(this, KEY_NOTIFICATION_CHANNEL_ERRORS)
                .setContentTitle(getString(R.string.notification_access_notification_title))
                .setContentText(getString(R.string.notification_access_notification_title_description))
                .setContentIntent(notificationManagerPendingIntent)
                .setSmallIcon(R.drawable.ic_notification)


        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_SERVICE_ERROR,
                notificationBuilder.build())
    }

    private fun onWatchSwipeExited() {
        if (!Preferences.getBoolean(preferences, MiscPreferences.PAUSE_ON_SWIPE_EXIT)) {
            return
        }

        if (currentMediaController?.isPlaying() == true) {
            currentMediaController?.transportControls?.pause()
        }
    }

    override fun onMessageReceived(event: MessageEvent?) {
        if (event == null) {
            return
        }

        when {
            event.path == CommPaths.MESSAGE_WATCH_CLOSED -> {
                stopSelf()
            }
            event.path == CommPaths.MESSAGE_ACK -> {
                ackTimeoutHandler.removeMessages(MESSAGE_STOP_SELF)
            }
            event.path == CommPaths.MESSAGE_CHANGE_VOLUME -> {
                updateVolume(FloatPacker.unpackFloat(event.data))
            }
            event.path == CommPaths.MESSAGE_EXECUTE_ACTION -> {
                executeAction(ButtonInfo(WatchActions.ProtoButtonInfo.parseFrom(event.data)))
            }
            event.path == CommPaths.MESSAGE_EXECUTE_MENU_ACTION -> {
                executeMenuAction(ByteBuffer.wrap(event.data).int)
            }
            event.path == CommPaths.MESSAGE_WATCH_OPENED -> {
                ackTimeoutHandler.removeMessages(MESSAGE_STOP_SELF)
                buildMusicStateAndTransmit(currentMediaController)
            }
            event.path == CommPaths.MESSAGE_WATCH_CLOSED_MANUALLY -> {
                onWatchSwipeExited()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val persistentChannel = NotificationChannel(KEY_NOTIFICATION_CHANNEL,
                getString(R.string.music_control),
                NotificationManager.IMPORTANCE_MIN)
        notificationManager.createNotificationChannel(persistentChannel)

        val errorChannel = NotificationChannel(KEY_NOTIFICATION_CHANNEL_ERRORS,
                getString(R.string.error_notifications),
                NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(errorChannel)
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