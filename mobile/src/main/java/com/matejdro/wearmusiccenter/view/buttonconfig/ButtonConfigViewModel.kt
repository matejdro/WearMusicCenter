package com.matejdro.wearmusiccenter.view.buttonconfig

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.matejdro.wearmusiccenter.config.ActionConfigProvider
import com.matejdro.wearmusiccenter.config.ActionConfigStorage
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.di.ConfigActivityComponent
import javax.inject.Inject

class ButtonConfigViewModel(setsPlaybackActions : Boolean, configActivityComponent: ConfigActivityComponent) : ViewModel() {
    @Inject
    lateinit var watchInfoProvider: WatchInfoProvider

    @Inject
    lateinit var buttonConfigProvider : ActionConfigProvider

    val buttonConfig = MutableLiveData<ActionConfigStorage>()

    init {
        configActivityComponent.inject(this)

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
}

class ButtonConfigViewModelFactory(private val setsPlaybackActions : Boolean, private val configActivityComponent: ConfigActivityComponent) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(p0: Class<T>?): T = ButtonConfigViewModel(setsPlaybackActions, configActivityComponent) as T

}