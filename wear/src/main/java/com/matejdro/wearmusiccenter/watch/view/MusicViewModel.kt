package com.matejdro.wearmusiccenter.watch.view

import android.app.Application
import android.arch.lifecycle.*
import android.content.SharedPreferences
import com.matejdro.wearmusiccenter.common.actions.StandardActions
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.watch.communication.PhoneConnection
import com.matejdro.wearmusiccenter.watch.config.ButtonAction
import com.matejdro.wearmusiccenter.watch.config.PreferencesBus
import com.matejdro.wearmusiccenter.watch.config.WatchActionConfigProvider
import com.matejdro.wearmusiccenter.watch.config.WatchActionMenuProvider
import com.matejdro.wearutils.lifecycle.Resource
import com.matejdro.wearutils.lifecycle.SingleLiveEvent
import timber.log.Timber

class MusicViewModel(application: Application?) : AndroidViewModel(application) {
    private val phoneConnection = PhoneConnection(getApplication())

    private val playbackConfig = WatchActionConfigProvider(phoneConnection.googleApiClient, phoneConnection.rawPlaybackConfig)
    private val stoppedConfig = WatchActionConfigProvider(phoneConnection.googleApiClient, phoneConnection.rawStoppedConfig)

    val currentButtonConfig = MediatorLiveData<WatchActionConfigProvider>()
    val musicState = MediatorLiveData<Resource<MusicState>>()
    val actionsMenuConfig = WatchActionMenuProvider(phoneConnection.googleApiClient, phoneConnection.rawActionMenuConfig)
    val preferences = PreferencesBus as LiveData<SharedPreferences>

    val volume = MutableLiveData<Float>()
    val popupVolumeBar = SingleLiveEvent<Unit>()
    val closeActionsMenu = SingleLiveEvent<Unit>()
    val openActionsMenu = SingleLiveEvent<Unit>()

    val albumArt
    get() = phoneConnection.albumArt

    fun executeActionFromMenu(index: Int) {
        val action = actionsMenuConfig.config.value?.get(index) ?: return

        closeActionsMenu.postValue(null)

        if (executeActionOnWatch(action)) {
            return
        }

        phoneConnection.executeMenuAction(index)
    }

    fun executeAction(buttonInfo: ButtonInfo) {
        val action = currentButtonConfig.value?.getAction(buttonInfo) ?: return
        if (!executeActionOnWatch(action)) {
            phoneConnection.executeButtonAction(buttonInfo)
        }
    }

    private fun executeActionOnWatch(action: ButtonAction): Boolean {
        return when {
            action.key == StandardActions.ACTION_VOLUME_UP -> {
                updateVolume(Math.min(1f, volume.value!! + (currentButtonConfig.value?.volumeStep ?: 0.1f)))
                popupVolumeBar.call()
                true
            }
            action.key == StandardActions.ACTION_VOLUME_DOWN -> {
                updateVolume(Math.max(0f, volume.value!! - (currentButtonConfig.value?.volumeStep ?: 0.1f)))
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

    fun updateVolume(newVolume : Float) {
        volume.value = newVolume
        phoneConnection.sendVolume(newVolume)
    }

    private val configChangeListener = Observer<WatchActionConfigProvider> {
        currentButtonConfig.value = it
    }

    private val musicStateListener = Observer<Resource<MusicState>> {
        val playing = it?.data?.playing == true

        val newMusicState = it?.data
        if (it?.status == Resource.Status.SUCCESS && newMusicState != null) {
            Timber.d("Volume value %f", newMusicState.volume)
            if (volume.value != newMusicState.volume) {
                volume.value = newMusicState.volume
            }
        }

        Timber.d("UpdateMusicConfig " + playing)

        val newConfig = if (playing) playbackConfig else stoppedConfig
        swapConfig(newConfig)

        musicState.value = it
    }

    private fun swapConfig(newConfig : WatchActionConfigProvider) {
        if (newConfig === currentButtonConfig.value) {
            return
        }

        currentButtonConfig.removeSource(currentButtonConfig.value?.updateListener)
        currentButtonConfig.addSource(newConfig.updateListener, configChangeListener)
    }

    override fun onCleared() {
        phoneConnection.close()
    }

    init {
        musicState.addSource(phoneConnection.musicState, musicStateListener)
        swapConfig(stoppedConfig)


        volume.value = 0.5f
    }

    fun close() {
        onCleared()
    }
}
