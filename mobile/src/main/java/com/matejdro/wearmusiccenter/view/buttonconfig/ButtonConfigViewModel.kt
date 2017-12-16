package com.matejdro.wearmusiccenter.view.buttonconfig

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.kakai.android.autoviewmodelfactory.annotations.AutoViewModelFactory
import com.matejdro.wearmusiccenter.config.ActionConfig
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.config.buttons.ButtonConfig
import com.matejdro.wearmusiccenter.di.LocalActivityConfig
import javax.inject.Named

@AutoViewModelFactory
class ButtonConfigViewModel(@Named(ARG_DISPLAY_PLAYBACK_ACTIONS) setsPlaybackActions: Boolean,
                            val watchInfoProvider: WatchInfoProvider,
                            @LocalActivityConfig val buttonConfigProvider: ActionConfig) : ViewModel() {

    val buttonConfig = MutableLiveData<ButtonConfig>()

    init {
        val config = if (setsPlaybackActions) {
            buttonConfigProvider.getPlayingConfig()
        } else {
            buttonConfigProvider.getStoppedConfig()
        }

        buttonConfig.value = config
    }

    fun commitConfig() {
        buttonConfig.value?.commit()
        buttonConfig.value = buttonConfig.value
    }

    companion object {
        const val ARG_DISPLAY_PLAYBACK_ACTIONS = "DisplayPlaybackActions"
    }
}