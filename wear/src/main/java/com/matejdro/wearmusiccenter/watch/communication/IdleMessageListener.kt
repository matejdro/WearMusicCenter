package com.matejdro.wearmusiccenter.watch.communication

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.watch.view.MainActivity

class IdleMessageListener : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            CommPaths.MESSAGE_OPEN_APP -> {
                val activityStartIntent = Intent(this, MainActivity::class.java)
                activityStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(activityStartIntent)
            }
            CommPaths.MESSAGE_START_SERVICE -> {
                startService(Intent(this, WatchMusicService::class.java))
            }
        }
    }
}
