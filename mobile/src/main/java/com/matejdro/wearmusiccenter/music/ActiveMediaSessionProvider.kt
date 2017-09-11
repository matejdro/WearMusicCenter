package com.matejdro.wearmusiccenter.music

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.matejdro.wearmusiccenter.DummyNotificationService
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearutils.lifecycle.Resource
import javax.inject.Inject

class ActiveMediaSessionProvider @Inject constructor(private val context: Context) :
        android.arch.lifecycle.LiveData<Resource<MediaController>>(),
        MediaSessionManager.OnActiveSessionsChangedListener {

    private val notificationListenerComponent: ComponentName =
            ComponentName(context, DummyNotificationService::class.java)

    private val mediaSessionManager: MediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    var currentController : MediaController? = null

    private val idlePlayers: ArrayList<OwnedPlaybackCallback>

    private fun findPlayingMediaController() {
        val activeSessions: MutableList<MediaController>
        try {
            activeSessions = mediaSessionManager.getActiveSessions(notificationListenerComponent)
        } catch(e: SecurityException) {
            value = Resource.error(context.getString(R.string.error_notification_access), null)
            return
        }

        val newController = activeSessions.firstOrNull { it.isPlaying() }

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

        setReportedController(reportedController)
    }

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        updateControllerIfNeeded()
    }

    override fun onActive() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(this, notificationListenerComponent)
        } catch(e: SecurityException) {
            value = Resource.error(context.getString(R.string.error_notification_access), null)
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

    private fun removeCurrentController() {
        currentController?.unregisterCallback(mediaCallback)
        currentController = null
    }

    private val mediaCallback: MediaController.Callback

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

    private fun setReportedController(mediaController: MediaController?) {
        value = if (mediaController == null) {
            null
        } else {
            Resource.success(mediaController)
        }
    }

    init {
        this.idlePlayers = ArrayList<OwnedPlaybackCallback>()
        this.mediaCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateControllerIfNeeded()
            }

            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                setReportedController(currentController)
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
