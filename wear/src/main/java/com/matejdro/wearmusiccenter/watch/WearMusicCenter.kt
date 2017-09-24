package com.matejdro.wearmusiccenter.watch

import android.preference.PreferenceManager
import com.matejdro.wearmusiccenter.watch.config.PreferencesBus
import com.matejdro.wearutils.logging.TimberExceptionWear
import pl.tajchert.exceptionwear.ExceptionWear
import timber.log.Timber

class WearMusicCenter : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        timber.log.Timber.setAppTag("WearMusicCenter")

        val isDebuggable = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        Timber.plant(timber.log.Timber.AndroidDebugTree(isDebuggable))

        if (isDebuggable) {
            ExceptionWear.initialize(this)
            Timber.plant(TimberExceptionWear(this))
        }

        PreferencesBus.value = PreferenceManager.getDefaultSharedPreferences(this)
    }
}