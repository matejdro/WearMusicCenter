package com.matejdro.wearmusiccenter.watch

class WearMusicCenter : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        timber.log.Timber.setAppTag("WearMusicCenter")

        val isDebuggable = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        timber.log.Timber.plant(timber.log.Timber.AndroidDebugTree(isDebuggable))
    }
}