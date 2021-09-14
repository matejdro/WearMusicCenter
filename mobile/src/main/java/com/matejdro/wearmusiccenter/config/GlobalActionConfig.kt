package com.matejdro.wearmusiccenter.config

import android.content.Context
import androidx.lifecycle.Observer
import com.matejdro.wearmusiccenter.config.actionlist.ActionList
import com.matejdro.wearmusiccenter.config.actionlist.GlobalActionList
import com.matejdro.wearmusiccenter.config.buttons.ButtonConfig
import com.matejdro.wearmusiccenter.config.buttons.GlobalButtonConfigFactory
import dagger.Lazy
import java.io.File

class GlobalActionConfig(private val context: Context,
                         private val watchInfoProvider: WatchInfoProvider,
                         private val defaultConfigGenerator: DefaultConfigGenerator,
                         actionListConfigLazy: Lazy<GlobalActionList>,
                         globalButtonConfigFactory: GlobalButtonConfigFactory) : ActionConfig {

    private val playingConfig by lazy { globalButtonConfigFactory.create(true) }
    private val stoppedConfig by lazy { globalButtonConfigFactory.create(false) }
    private val actionListConfig by lazy { actionListConfigLazy.get() }

    override fun getPlayingConfig(): ButtonConfig = playingConfig
    override fun getStoppedConfig(): ButtonConfig = stoppedConfig
    override fun getActionList(): ActionList = actionListConfig

    private fun doesConfigExist(): Boolean =
            File(context.filesDir, "action_config_playing").exists() &&
                    File(context.filesDir, "action_config_stopped").exists() &&
                    File(context.filesDir, "actions_list").exists()

    private val defaultConfigCreatorListener = object : Observer<WatchInfoWithIcons?> {
        override fun onChanged(t: WatchInfoWithIcons?) {
            watchInfoProvider.removeObserver(this)
            defaultConfigGenerator.generateDefaultButtons(this@GlobalActionConfig)
            defaultConfigGenerator.generateDefaultActionList(getActionList())

            playingConfig.commit()
            stoppedConfig.commit()
            actionListConfig.commit()
        }
    }


    init {
        if (!doesConfigExist()) {
            watchInfoProvider.observeForever(defaultConfigCreatorListener)
        }
    }

}
