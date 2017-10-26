package com.matejdro.wearmusiccenter.watch.communication

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.watch.view.MainActivity

class IdleMessageListener : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (CommPaths.MESSAGE_OPEN_APP == messageEvent.path) {
            val activityStartIntent = Intent(this, MainActivity::class.java)
            activityStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(activityStartIntent)
        }
    }
}
