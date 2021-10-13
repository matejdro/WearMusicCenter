package com.matejdro.wearmusiccenter.watch

import android.content.pm.ApplicationInfo
import android.preference.PreferenceManager
import com.matejdro.wearmusiccenter.watch.config.PreferencesBus
import com.matejdro.wearutils.logging.FileLogger
import com.matejdro.wearutils.logging.TimberExceptionWear
import dagger.hilt.android.HiltAndroidApp
import pl.tajchert.exceptionwear.ExceptionWear
import timber.log.Timber


@HiltAndroidApp
class WearMusicCenter : android.app.Application() {
    override fun onCreate() {
        super.onCreate()


        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        Timber.setAppTag("WearMusicCenter")
        Timber.plant(Timber.AndroidDebugTree(isDebuggable))

        if (!isDebuggable) {
            ExceptionWear.initialize(this)
            Timber.plant(TimberExceptionWear(this))
        }

        val fileLogger = FileLogger.getInstance(this)
        fileLogger.activate()
        Timber.plant(fileLogger)

        PreferencesBus.value = PreferenceManager.getDefaultSharedPreferences(this)
    }
}