package com.matejdro.wearmusiccenter.watch

import android.preference.PreferenceManager
import com.matejdro.wearmusiccenter.watch.config.PreferencesBus

class WearMusicCenter : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        timber.log.Timber.setAppTag("WearMusicCenter")

        val isDebuggable = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        timber.log.Timber.plant(timber.log.Timber.AndroidDebugTree(isDebuggable))

        PreferencesBus.value = PreferenceManager.getDefaultSharedPreferences(this)
    }
}