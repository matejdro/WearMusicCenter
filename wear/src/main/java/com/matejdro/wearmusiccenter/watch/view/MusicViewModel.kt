package com.matejdro.wearmusiccenter.watch.view

import android.app.Application
import android.content.SharedPreferences
import android.os.Handler
import androidx.lifecycle.*
import com.matejdro.wearmusiccenter.common.MiscPreferences
import com.matejdro.wearmusiccenter.common.actions.StandardActions
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.buttonconfig.SpecialButtonCodes
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.watch.communication.CustomListWithBitmaps
import com.matejdro.wearmusiccenter.watch.communication.PhoneConnection
import com.matejdro.wearmusiccenter.watch.config.ButtonAction
import com.matejdro.wearmusiccenter.watch.config.PreferencesBus
import com.matejdro.wearmusiccenter.watch.config.WatchActionConfigProvider
import com.matejdro.wearmusiccenter.watch.config.WatchActionMenuProvider
import com.matejdro.wearmusiccenter.watch.model.Notification
import com.matejdro.wearutils.lifecycle.Resource
import com.matejdro.wearutils.lifecycle.SingleLiveEvent
import com.matejdro.wearutils.preferences.definition.Preferences
import timber.log.Timber

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val phoneConnection = PhoneConnection(getApplication())

    private val playbackConfig = WatchActionConfigProvider(phoneConnection.googleApiClient, phoneConnection.rawPlaybackConfig)
    private val stoppedConfig = WatchActionConfigProvider(phoneConnection.googleApiClient, phoneConnection.rawStoppedConfig)

    private val handler = Handler()
    private var closeDeadline = Long.MAX_VALUE

    val currentButtonConfig = MediatorLiveData<WatchActionConfigProvider>()
    val musicState = MediatorLiveData<Resource<MusicState>>()
    val customList = MediatorLiveData<CustomListWithBitmaps>()
    val actionsMenuConfig = WatchActionMenuProvider(phoneConnection.googleApiClient, phoneConnection.rawActionMenuConfig)
    val preferences = PreferencesBus as LiveData<SharedPreferences>

    val notification: LiveData<Notification> = phoneConnection.notification

    val volume = MutableLiveData<Float>()
    val popupVolumeBar = SingleLiveEvent<Unit>()
    val closeActionsMenu = SingleLiveEvent<Unit>()
    val openActionsMenu = SingleLiveEvent<Unit>()
    val closeApp = SingleLiveEvent<Unit>()

    val albumArt
        get() = phoneConnection.albumArt

    fun updateTimers() {
        if (closeDeadline < System.currentTimeMillis()) {
            closeApp.call()
        }
    }

    fun executeActionFromMenu(index: Int) {
        val action = actionsMenuConfig.config.value?.get(index) ?: return

        closeActionsMenu.postValue(null)

        if (executeActionOnWatch(action, 1f)) {
            return
        }

        phoneConnection.executeMenuAction(index)
    }

    fun executeItemFromCustomMenu(listId: String, itemId: String) {
        closeActionsMenu.postValue(null)
        phoneConnection.executeCustomMenuAction(listId, itemId)
    }

    fun executeAction(buttonInfo: ButtonInfo): Boolean {
        val action = currentButtonConfig.value?.getAction(buttonInfo) ?: return false

        val multiplier = if (buttonInfo.buttonCode == SpecialButtonCodes.TURN_ROTARY_CW ||
                buttonInfo.buttonCode == SpecialButtonCodes.TURN_ROTARY_CCW) {
            Preferences.getInt(preferences.value!!, MiscPreferences.ROTATING_CROWN_SENSITIVITY) / 100f
        } else {
            1f
        }

        if (!executeActionOnWatch(action, multiplier)) {
            phoneConnection.executeButtonAction(buttonInfo)
        }

        return true
    }

    private fun executeActionOnWatch(action: ButtonAction, multiplier: Float): Boolean {
        return when {
            action.key == StandardActions.ACTION_VOLUME_UP -> {
                val volumeStep = (currentButtonConfig.value?.volumeStep ?: 0.1f) * multiplier
                updateVolume(Math.min(1f, volume.value!! + volumeStep))
                popupVolumeBar.call()
                true
            }
            action.key == StandardActions.ACTION_VOLUME_DOWN -> {
                val volumeStep = (currentButtonConfig.value?.volumeStep ?: 0.1f) * multiplier

                updateVolume(Math.max(0f, volume.value!! - volumeStep))
                popupVolumeBar.call()
                true
            }
            action.key == StandardActions.ACTION_OPEN_MENU -> {
                openActionsMenu.call()
                true
            }

            else -> false
        }
    }

    fun updateVolume(newVolume: Float) {
        volume.value = newVolume
        phoneConnection.sendVolume(newVolume)
    }

    fun sendManualCloseMessage() {
        phoneConnection.sendManualCloseMessage()
    }

    fun openPlaybackQueue() {
        phoneConnection.openPlaybackQueue()
    }

    private val configChangeListener = Observer<WatchActionConfigProvider> {
        currentButtonConfig.value = it
    }

    private val musicStateListener = Observer<Resource<MusicState>> {
        Timber.d("Received MusicState %s", it?.data)
        val playing = it?.data?.playing == true

        closeDeadline = Long.MAX_VALUE
        handler.removeCallbacks(closeRunnable)
        if (!playing) {
            val timeout = Preferences.getInt(preferences.value!!, MiscPreferences.CLOSE_TIMEOUT)
            if (timeout > 0) {
                val timeoutMs = timeout * 1000L
                closeDeadline = System.currentTimeMillis() + timeoutMs
                handler.postDelayed(closeRunnable, timeoutMs)
            }
        }

        val newMusicState = it?.data
        if (it?.status == Resource.Status.SUCCESS && newMusicState != null) {
            if (volume.value != newMusicState.volume) {
                volume.value = newMusicState.volume
            }
        }

        val newConfig = if (playing) playbackConfig else stoppedConfig
        swapConfig(newConfig)

        musicState.value = it
    }

    private fun swapConfig(newConfig: WatchActionConfigProvider) {
        if (newConfig === currentButtonConfig.value) {
            return
        }


        currentButtonConfig.value?.updateListener?.let { currentButtonConfig.removeSource(it) }
        currentButtonConfig.addSource(newConfig.updateListener, configChangeListener)
    }

    override fun onCleared() {
        phoneConnection.close()
    }

    init {
        musicState.addSource(phoneConnection.musicState, musicStateListener)
        musicState.addSource(phoneConnection.customList) { customList.value = it }
        swapConfig(stoppedConfig)


        volume.value = 0.5f
    }

    fun close() {
        onCleared()
    }

    private val closeRunnable = Runnable {
        phoneConnection.stop()
        closeApp.call()
    }
}
