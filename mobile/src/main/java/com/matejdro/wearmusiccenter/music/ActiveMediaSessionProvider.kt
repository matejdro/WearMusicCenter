package com.matejdro.wearmusiccenter.music

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.matejdro.wearmusiccenter.DummyNotificationService
import timber.log.Timber
import javax.inject.Inject

class ActiveMediaSessionProvider @Inject constructor(context: Context) : android.arch.lifecycle.LiveData<MediaController?>(), MediaSessionManager.OnActiveSessionsChangedListener {

    val notificationListenerComponent: ComponentName =
            ComponentName(context, DummyNotificationService::class.java)

    val mediaSessionManager: MediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    var currentController : MediaController? = null

    val idlePlayers: ArrayList<OwnedPlaybackCallback>

    private fun findPlayingMediaController() {
        val activeSessions: MutableList<MediaController>
        try {
            activeSessions = mediaSessionManager.getActiveSessions(notificationListenerComponent)
        } catch(e: SecurityException) {
            Timber.e("Notification service not active!")
            return
        }

        val newController = activeSessions
                .filter {it.isPlaying()}
                .firstOrNull()

        var reportedController = newController
        if (newController == null) {
            reportedController = currentController
        }

        removeCurrentController()
        currentController = newController

        idlePlayers.forEach(OwnedPlaybackCallback::unregister)
        idlePlayers.clear()

        if (currentController == null) {
            activeSessions.forEach {
                idlePlayers.add(OwnedPlaybackCallback((it)))
            }
        } else {
            currentController?.registerCallback(mediaCallback)
        }

        value = reportedController
    }

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        updateControllerIfNeeded()
    }

    override fun onActive() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(this, notificationListenerComponent)
        } catch(e: SecurityException) {
            Timber.e("Notification service not active!")
        }

        updateControllerIfNeeded()
    }

    override fun onInactive() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(this)

        removeCurrentController()
        idlePlayers.forEach(OwnedPlaybackCallback::unregister)
        idlePlayers.clear()
    }

    fun updateControllerIfNeeded() {
        if (currentController?.isPlaying() != true) {
            findPlayingMediaController()
        }
    }

    fun removeCurrentController() {
        currentController?.unregisterCallback(mediaCallback)
        currentController = null
    }

    val mediaCallback: MediaController.Callback

    inner class OwnedPlaybackCallback(val controller : MediaController) : MediaController.Callback() {
        init {
            controller.registerCallback(this)
        }

        fun unregister() {
            controller.unregisterCallback(this)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state?.isPlaying() == true) {
                updateControllerIfNeeded()
            }
        }

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo?) {
            updateControllerIfNeeded()
        }
    }

    init {
        this.idlePlayers = ArrayList<OwnedPlaybackCallback>()
        this.mediaCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateControllerIfNeeded()
            }

            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                value = currentController
            }
        }
    }
}

fun PlaybackState.isPlaying() : Boolean {
    val state = this.state
    return state != PlaybackState.STATE_NONE &&
            state != PlaybackState.STATE_PAUSED &&
            state != PlaybackState.STATE_STOPPED &&
            state != PlaybackState.STATE_ERROR
}


fun MediaController.isPlaying() : Boolean {
    return this.playbackState?.isPlaying() == true
}
