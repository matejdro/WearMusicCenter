package com.matejdro.wearmusiccenter

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.session.MediaController
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.lifecycle.Observer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.MiscPreferences
import com.matejdro.wearmusiccenter.common.model.AutoStartMode
import com.matejdro.wearmusiccenter.music.ActiveMediaSessionProvider
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.music.isPlaying
import com.matejdro.wearutils.lifecycle.Resource
import com.matejdro.wearutils.messages.sendMessageToNearestClient
import com.matejdro.wearutils.preferences.definition.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class NotificationService : NotificationListenerService() {
    private lateinit var preferences: SharedPreferences
    private var bound = false

    private var activeMediaProvider: ActiveMediaSessionProvider? = null

    private val coroutineScope = CoroutineScope(Job())
    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient

    override fun onCreate() {
        super.onCreate()

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        nodeClient = Wearable.getNodeClient(applicationContext)
        messageClient = Wearable.getMessageClient(applicationContext)

        Timber.d("Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ACTION_UNBIND_SERVICE == intent?.action &&
                bound &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestUnbindSafe()
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
            requestUnbindSafe()
            return
        }

        activeMediaProvider = ActiveMediaSessionProvider(this)

        if (MiscPreferences.isAnyKindOfAutoStartEnabled(preferences)) {
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
        coroutineScope.cancel()

        super.onDestroy()
    }

    private fun shouldRun(): Boolean {
        return MiscPreferences.isAnyKindOfAutoStartEnabled(preferences)
    }

    private fun startAppOnWatch() {
        Timber.d("AttemptToStartApp")
        coroutineScope.launch {
            try {
                val legacySetting = Preferences.getBoolean(preferences, MiscPreferences.AUTO_START)
                val openType = if (legacySetting) {
                    AutoStartMode.OPEN_APP
                } else {
                    Preferences.getEnum(preferences, MiscPreferences.AUTO_START_MODE)
                }

                val message = when (openType) {
                    AutoStartMode.OFF, null -> return@launch
                    AutoStartMode.SHOW_ICON -> CommPaths.MESSAGE_START_SERVICE
                    AutoStartMode.OPEN_APP -> CommPaths.MESSAGE_OPEN_APP
                }

                messageClient.sendMessageToNearestClient(nodeClient, message)
                Timber.d("Start success")
            } catch (e: Exception) {
                Timber.e(e, "Start Fail")
            }
        }
    }

    private val mediaObserver = Observer<Resource<MediaController>?> {
        Timber.d("Playback update %b %s", MusicService.active, it?.data?.playbackState?.state)

        if (!MusicService.active && it?.data?.playbackState?.isPlaying() == true) {
            val autoStartBlacklist = Preferences.getStringSet(preferences, MiscPreferences.AUTO_START_APP_BLACKLIST)
            if (!autoStartBlacklist.contains(it.data?.packageName)) {
                startAppOnWatch()
            }
        }
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, NotificationService::class.java)
            val enabledListeners = Settings.Secure.getString(context.contentResolver,
                    "enabled_notification_listeners")

            return enabledListeners != null && enabledListeners.contains(component.flattenToString())
        }

        const val ACTION_UNBIND_SERVICE = "UNBIND"
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun requestUnbindSafe() {
        try {
            requestUnbind()
        } catch (e: SecurityException) {
            // Sometimes notification service may be unbound before we can unbind safely.
            // just stop self.
            stopSelf()
        }
    }
}
