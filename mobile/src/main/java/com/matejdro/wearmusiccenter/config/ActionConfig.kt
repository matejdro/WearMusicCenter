package com.matejdro.wearmusiccenter.config

import com.matejdro.wearmusiccenter.config.actionlist.ActionList
import com.matejdro.wearmusiccenter.config.buttons.ButtonConfig

interface ActionConfig {
    fun getPlayingConfig(): ButtonConfig
    fun getStoppedConfig(): ButtonConfig
    fun getActionList(): ActionList
}