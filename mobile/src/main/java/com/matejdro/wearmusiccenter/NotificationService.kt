package com.matejdro.wearmusiccenter

import android.arch.lifecycle.Observer
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.session.MediaController
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.MiscPreferences
import com.matejdro.wearmusiccenter.music.ActiveMediaSessionProvider
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.music.isPlaying
import com.matejdro.wearutils.lifecycle.Resource
import com.matejdro.wearutils.messages.MessagingUtils
import com.matejdro.wearutils.preferences.definition.Preferences
import timber.log.Timber

class NotificationService : NotificationListenerService() {
    lateinit var preferences: SharedPreferences
    private var bound = false

    private var activeMediaProvider: ActiveMediaSessionProvider? = null
    private var googleApiClient: GoogleApiClient? = null

    override fun onCreate() {
        super.onCreate()

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        Timber.d("Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ACTION_UNBIND_SERVICE == intent?.action &&
                bound &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestUnbind()
            Timber.d("Unbind on command")
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        Timber.d("Listener connected")

        val musicServiceNotifyIntent = Intent(this, MusicService::class.java)
        musicServiceNotifyIntent.action = MusicService.ACTION_NOTIFICATION_SERVICE_ACTIVATED
        startService(musicServiceNotifyIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !shouldRun()) {
            //Running notification service is not needed for this app to run, it only needs to be enabled.

            Timber.d("Unbind on start")

            // On N+ we can turn off the service
            requestUnbind()
            return
        }

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build()
        googleApiClient!!.connect()

        activeMediaProvider = ActiveMediaSessionProvider(this)

        if (Preferences.getBoolean(preferences, MiscPreferences.AUTO_START)) {
            activeMediaProvider!!.observeForever(mediaObserver)
        }

        bound = true
    }

    override fun onListenerDisconnected() {
        Timber.d("Listener disconnected")
        activeMediaProvider?.removeObserver(mediaObserver)

        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        Timber.d("Service destroyed")

        activeMediaProvider?.removeObserver(mediaObserver)

        super.onDestroy()
    }

    private fun shouldRun(): Boolean {
        return Preferences.getBoolean(preferences, MiscPreferences.AUTO_START)
    }

    private fun startAppOnWatch() {
        Timber.d("AttemptToStartApp %s", googleApiClient?.isConnected)
        if (googleApiClient?.isConnected == true) {
            Timber.d("Starting app")
            MessagingUtils.sendSingleMessage(googleApiClient,
                    CommPaths.MESSAGE_OPEN_APP,
                    null) {
                Timber.d("Start success: %b", it.status.isSuccess)
            }
        }
    }

    private val mediaObserver = Observer<Resource<MediaController>> {
        Timber.d("Playback update %b %s", MusicService.active, it?.data?.playbackState?.state)

        if (!MusicService.active && it?.data?.playbackState?.isPlaying() == true) {
            startAppOnWatch()
        }
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, NotificationService::class.java)
            val enabledListeners = Settings.Secure.getString(context.contentResolver,
                    "enabled_notification_listeners")

            return enabledListeners != null && enabledListeners.contains(component.flattenToString())
        }

        val ACTION_UNBIND_SERVICE = "UNBIND"
    }
}
