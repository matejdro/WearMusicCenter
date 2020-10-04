package com.matejdro.wearmusiccenter.notifications

import androidx.lifecycle.LiveData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.matejdro.wearvibrationcenter.notificationprovider.INotificationListener
import com.matejdro.wearvibrationcenter.notificationprovider.INotificationProvider
import com.matejdro.wearvibrationcenter.notificationprovider.NotificationProviderConstants
import com.matejdro.wearvibrationcenter.notificationprovider.ReceivedNotification
import timber.log.Timber
import javax.inject.Inject

class NotificationProvider @Inject constructor(private val context: Context) : LiveData<ReceivedNotification>() {
    var providerService: INotificationProvider? = null

    override fun onActive() {
        val intent = Intent()
        intent.component = NotificationProviderConstants.TARGET_COMPONENT
        intent.action = NotificationProviderConstants.ACTION_NOTIFICATION_PROVIDER

        try {
            context.bindService(intent, notificationServiceConnection, 0)
        } catch (e: Exception) {
            Timber.e(e, "Notification binding error")
        }
    }

    override fun onInactive() {
        context.unbindService(notificationServiceConnection)
    }

    private val notificationServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            providerService = INotificationProvider.Stub.asInterface(service)
            providerService!!.startSendingNotifications(listener)
        }
    }

    private val listener = object : INotificationListener.Stub() {
        override fun onNotificationReceived(notification: ReceivedNotification) {
            postValue(notification)
        }
    }
}
