package com.matejdro.wearmusiccenter

import android.os.Build
import android.service.notification.NotificationListenerService

class DummyNotificationService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //Running notification service is not needed for this app to run, it only needs to be enabled.

            // On N+ we can turn off the service
            requestUnbind()
        }
    }
}
