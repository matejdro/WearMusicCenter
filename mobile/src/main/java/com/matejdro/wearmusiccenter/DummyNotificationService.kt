package com.matejdro.wearmusiccenter

import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import com.matejdro.wearmusiccenter.music.MusicService

class DummyNotificationService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()

        val musicServiceNotifyIntent = Intent(this, MusicService::class.java)
        musicServiceNotifyIntent.action = MusicService.ACTION_NOTIFICATION_SERVICE_ACTIVATED
        startService(musicServiceNotifyIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //Running notification service is not needed for this app to run, it only needs to be enabled.

            // On N+ we can turn off the service
            requestUnbind()
        }
    }
}
