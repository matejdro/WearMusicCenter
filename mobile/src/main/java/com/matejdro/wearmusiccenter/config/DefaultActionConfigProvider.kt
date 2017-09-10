package com.matejdro.wearmusiccenter.config

import android.arch.lifecycle.Observer
import android.content.Context
import com.matejdro.wearmusiccenter.config.actionlist.ActionListStorage
import com.matejdro.wearmusiccenter.config.actionlist.DefaultActionListStorage
import com.matejdro.wearmusiccenter.config.buttons.ActionConfigStorage
import com.matejdro.wearmusiccenter.config.buttons.DefaultActionConfigStorage
import java.io.File

class DefaultActionConfigProvider(private val context: Context,
                                  private val watchInfoProvider: WatchInfoProvider,
                                  private val defaultConfigGenerator: DefaultConfigGenerator) : ActionConfigProvider {

    private val playingConfig by lazy { DefaultActionConfigStorage(true, context, watchInfoProvider) }
    private val stoppedConfig by lazy { DefaultActionConfigStorage(false, context, watchInfoProvider) }
    private val actionListConfig by lazy { DefaultActionListStorage(context, watchInfoProvider) }

    override fun getPlayingConfig(): ActionConfigStorage = playingConfig
    override fun getStoppedConfig(): ActionConfigStorage = stoppedConfig
    override fun getActionList(): ActionListStorage = actionListConfig

    private fun doesConfigExist(): Boolean =
            File(context.filesDir, "action_config_playing").exists() &&
                    File(context.filesDir, "action_config_stopped").exists() &&
                    File(context.filesDir, "actions_list").exists()

    private val defaultConfigCreatorListener = object : Observer<WatchInfoWithIcons> {
        override fun onChanged(t: WatchInfoWithIcons?) {
            watchInfoProvider.removeObserver(this)
            defaultConfigGenerator.generateDefaultButtons(this@DefaultActionConfigProvider)
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