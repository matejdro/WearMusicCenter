package com.matejdro.wearmusiccenter.config

import android.content.Context

class DefaultActionConfigProvider (private val context: Context, private val watchInfoProvider: WatchInfoProvider) : ActionConfigProvider {
    private val playingConfig by lazy {DefaultActionConfigStorage(true, context, watchInfoProvider)}
    private val stoppedConfig by lazy {DefaultActionConfigStorage(false, context, watchInfoProvider)}

    override fun getPlayingConfig(): ActionConfigStorage = playingConfig
    override fun getStoppedConfig(): ActionConfigStorage = stoppedConfig
}