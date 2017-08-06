package com.matejdro.wearmusiccenter.config

import com.matejdro.wearmusiccenter.config.actionlist.ActionListStorage
import com.matejdro.wearmusiccenter.config.buttons.ActionConfigStorage

interface ActionConfigProvider {
    fun getPlayingConfig() : ActionConfigStorage
    fun getStoppedConfig() : ActionConfigStorage
    fun getActionList(): ActionListStorage
}