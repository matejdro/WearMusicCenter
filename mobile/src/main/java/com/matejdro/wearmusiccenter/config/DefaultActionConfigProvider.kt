package com.matejdro.wearmusiccenter.config

import android.content.Context
import com.matejdro.wearmusiccenter.config.actionlist.ActionListStorage
import com.matejdro.wearmusiccenter.config.actionlist.DefaultActionListStorage
import com.matejdro.wearmusiccenter.config.buttons.ActionConfigStorage
import com.matejdro.wearmusiccenter.config.buttons.DefaultActionConfigStorage

class DefaultActionConfigProvider (private val context: Context, private val watchInfoProvider: WatchInfoProvider) : ActionConfigProvider {

    private val playingConfig by lazy { DefaultActionConfigStorage(true, context, watchInfoProvider) }
    private val stoppedConfig by lazy { DefaultActionConfigStorage(false, context, watchInfoProvider) }
    private val actionListConfig by lazy { DefaultActionListStorage(context) }

    override fun getPlayingConfig(): ActionConfigStorage = playingConfig
    override fun getStoppedConfig(): ActionConfigStorage = stoppedConfig
    override fun getActionList(): ActionListStorage = actionListConfig

}