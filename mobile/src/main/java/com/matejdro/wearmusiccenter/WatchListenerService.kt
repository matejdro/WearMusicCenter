package com.matejdro.wearmusiccenter

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.matejdro.wearmusiccenter.music.MusicService

class WatchListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent?) {
        if (event == null) {
            return
        }

        startService(Intent(this, MusicService::class.java))
    }
}