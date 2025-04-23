package com.matejdro.wearmusiccenter.music

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.matejdro.wearmusiccenter.NotificationService
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearutils.lifecycle.Resource
import timber.log.Timber
import javax.inject.Inject

class ActiveMediaSessionProvider @Inject constructor(private val context: Context) :
        androidx.lifecycle.LiveData<Resource<MediaController>>(),
        MediaSessionManager.OnActiveSessionsChangedListener {

    private val notificationListenerComponent: ComponentName =
            ComponentName(context, NotificationService::class.java)

    private val mediaSessionManager: MediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    var currentController : MediaController? = null

    private val idlePlayers: ArrayList<OwnedPlaybackCallback>

    private fun findPlayingMediaController() {
        val activeSessions = getActiveSessions()
        Timber.d("Active Sessions %s", activeSessions.map { "${it.packageName} ${it.playbackState} ${it.playbackInfo}" })

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

        Timber.d("Reported session %s", activeSessions.map { "${reportedController?.packageName} ${reportedController?.playbackState} ${reportedController?.playbackInfo}" })
        setReportedController(reportedController)
    }

    private fun getActiveSessions(): List<MediaController> {
        return try {
            mediaSessionManager.getActiveSessions(notificationListenerComponent)
        } catch (e: SecurityException) {
            value = Resource.error(context.getString(R.string.error_notification_access), null)
            emptyList()
        }
    }

    fun activate() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(this, notificationListenerComponent)
        } catch (e: SecurityException) {
            value = Resource.error(context.getString(R.string.error_notification_access), null)
        }

        updateControllerIfNeeded()
    }

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        Timber.d("ActiveSessions changed %s", controllers?.map { it.packageName + " " + it.isPlaying() })
        updateControllerIfNeeded()
    }

    override fun onActive() {
        activate()
    }

    override fun onInactive() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(this)

        removeCurrentController()
        idlePlayers.forEach(OwnedPlaybackCallback::unregister)
        idlePlayers.clear()
    }

    fun updateControllerIfNeeded() {
        if (!isCurrentControllerActive() || currentController?.isPlaying() != true) {
            findPlayingMediaController()
        }
    }

    private fun isCurrentControllerActive(): Boolean {
        val currentController = currentController ?: return false

        return getActiveSessions().any { it.packageName == currentController.packageName }
    }

    private fun removeCurrentController() {
        currentController?.unregisterCallback(mediaCallback)
        currentController = null
    }

    private val mediaCallback: MediaController.Callback

    inner class OwnedPlaybackCallback(private val controller: MediaController) : MediaController.Callback() {
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

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) {
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
        this.idlePlayers = ArrayList()
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
