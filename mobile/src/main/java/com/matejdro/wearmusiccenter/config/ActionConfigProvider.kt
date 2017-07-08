package com.matejdro.wearmusiccenter.config

interface ActionConfigProvider {
    fun getPlayingConfig() : ActionConfigStorage
    fun getStoppedConfig() : ActionConfigStorage
}