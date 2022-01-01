package com.matejdro.wearmusiccenter.watch.communication

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.watch.view.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WatchMusicService : LifecycleService() {
    @Inject
    internal lateinit var phoneConnection: PhoneConnection

    private val uiFlow = MutableSharedFlow<Unit>()

    private var serviceTimeoutJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        createWearNotification()

        lifecycleScope.launch {
            val uiOpenFlow = uiFlow.subscriptionCount
                    .map { it > 0 }

            val musicPlayingFlow = phoneConnection.musicState.asFlow()
                    .map { it.data?.playing == true }

            combine(uiOpenFlow, musicPlayingFlow) { uiOpen, musicPlaying ->
                Timber.d("Service state UI open: %s Music playing: %s", uiOpen, musicPlaying)
                uiOpen || musicPlaying
            }
                    .distinctUntilChanged()
                    .collect { isActive ->
                        serviceTimeoutJob?.cancel()

                        if (isActive) {
                            createWearNotification()
                        } else {
                            removeWearNotification()
                            startTimeout()
                        }
                    }
        }
    }

    private fun createWearNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java)

        val openAppPendingIntent = PendingIntent.getActivity(this,
                1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)


        createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(this, KEY_NOTIFICATION_CHANNEL)
                .setContentTitle(getString(R.string.music_control_active))
                .setContentIntent(openAppPendingIntent)
                .setSmallIcon(R.drawable.ic_notification_white)
                .setOngoing(true)


        val ongoingActivity = OngoingActivity.Builder(this, NOTIFICATION_ID_PERSISTENT, notificationBuilder)
                .setStaticIcon(R.drawable.ic_notification_white)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setTouchIntent(openAppPendingIntent)
                .build()

        ongoingActivity.apply(this)

        startForeground(NOTIFICATION_ID_PERSISTENT, notificationBuilder.build())
    }

    private fun removeWearNotification() {
        stopForeground(true)
    }

    private fun startTimeout() {
        serviceTimeoutJob = lifecycleScope.launch {
            delay(SERVICE_TIMEOUT)

            stopSelf()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

        startService(Intent(this, WatchMusicService::class.java))

        return Binder(this)
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
    }

    class Binder(private val service: WatchMusicService) : android.os.Binder() {
        val uiOpenFlow: Flow<Unit>
            get() = service.uiFlow
    }
}


private const val NOTIFICATION_ID_PERSISTENT = 1
private const val KEY_NOTIFICATION_CHANNEL = "Service_Channel"

private const val SERVICE_TIMEOUT = 30_000L
