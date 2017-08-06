package com.matejdro.wearmusiccenter.watch.view

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.matejdro.wearmusiccenter.common.actions.CommonActions
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.watch.communication.PhoneConnection
import com.matejdro.wearmusiccenter.watch.config.ButtonAction
import com.matejdro.wearmusiccenter.watch.config.WatchActionConfigProvider
import com.matejdro.wearmusiccenter.watch.config.WatchActionMenuProvider
import com.matejdro.wearutils.lifecycle.Resource
import timber.log.Timber

class MusicViewModel(application: Application?) : AndroidViewModel(application) {
    private val phoneConnection = PhoneConnection(getApplication())

    private val playbackConfig = WatchActionConfigProvider(phoneConnection.googleApiClient, phoneConnection.rawPlaybackConfig)
    private val stoppedConfig = WatchActionConfigProvider(phoneConnection.googleApiClient, phoneConnection.rawStoppedConfig)

    val currentConfig = MediatorLiveData<WatchActionConfigProvider>()
    val musicState = MediatorLiveData<Resource<MusicState>>()
    val actionsMenuConfig = WatchActionMenuProvider(phoneConnection.googleApiClient, phoneConnection.rawActionMenuConfig)

    val volume = MutableLiveData<Float>()
    val popupVolumeBar = MutableLiveData<Unit>()
    val closeActionsMenu = MutableLiveData<Unit>()
    val openActionsMenu = MutableLiveData<Unit>()

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
        val action = currentConfig.value?.getAction(buttonInfo) ?: return
        if (!executeActionOnWatch(action)) {
            phoneConnection.executeButtonAction(buttonInfo)
        }
    }

    private fun executeActionOnWatch(action: ButtonAction): Boolean {
        return when {
            action.key == CommonActions.ACTION_VOLUME_UP -> {
                updateVolume(Math.min(1f, volume.value!! + (currentConfig.value?.volumeStep ?: 0.1f)))
                popupVolumeBar.value = popupVolumeBar.value
                true
            }
            action.key == CommonActions.ACTION_VOLUME_DOWN -> {
                updateVolume(Math.max(0f, volume.value!! - (currentConfig.value?.volumeStep ?: 0.1f)))
                popupVolumeBar.value = popupVolumeBar.value
                true
            }
            action.key == CommonActions.ACTION_OPEN_MENU -> {
                openActionsMenu.value = openActionsMenu.value
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
        currentConfig.value = it
    }

    private val musicStateListener = Observer<Resource<MusicState>> {
        val playing = it?.data?.playing ?: false

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
        Timber.d("SwapConfig %b %b %b %b", newConfig === playbackConfig, newConfig === playbackConfig, currentConfig.value === newConfig, currentConfig.value === null)
        if (newConfig === currentConfig.value) {
            return
        }

        currentConfig.removeSource(currentConfig.value?.updateListener)
        currentConfig.addSource(newConfig.updateListener, configChangeListener)
    }

    override fun onCleared() {
        phoneConnection.close()
    }

    init {
        musicState.addSource(phoneConnection.musicState, musicStateListener)
        swapConfig(stoppedConfig)

        currentConfig.observeForever {
            Timber.d("CurrentConfigChange %b %b", it === playbackConfig, it == null)
        }

        volume.value = 0.5f
    }

    fun close() {
        onCleared()
    }
}
