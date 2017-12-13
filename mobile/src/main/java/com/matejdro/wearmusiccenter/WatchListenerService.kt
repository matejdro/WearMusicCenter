package com.matejdro.wearmusiccenter

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.music.MusicService

class WatchListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent?) {
        if (event == null ||
                event.path == CommPaths.MESSAGE_WATCH_CLOSED ||
                event.path == CommPaths.MESSAGE_ACK ||
                event.path == CommPaths.MESSAGE_WATCH_CLOSED_MANUALLY) {

            return
        }

        val musicServiceIntent = Intent(this, MusicService::class.java)
        musicServiceIntent.action = MusicService.ACTION_START_FROM_WATCH
        startService(musicServiceIntent)
    }
}