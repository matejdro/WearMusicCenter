package com.matejdro.wearmusiccenter

import android.app.Application
import android.content.pm.ApplicationInfo
import com.matejdro.wearmusiccenter.di.AppComponent
import com.matejdro.wearmusiccenter.di.AppModule
import com.matejdro.wearmusiccenter.di.DaggerAppComponent
import timber.log.Timber
class WearMusicCenter : Application() {
    private lateinit var diComponent : AppComponent
    override fun onCreate() {
        super.onCreate()

        Timber.setAppTag("WearMusicCenter")

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        Timber.plant(Timber.AndroidDebugTree(isDebuggable))

        diComponent = DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()
        instance = this
    }

    companion object {
        private lateinit var instance : WearMusicCenter

        fun getAppComponent() : AppComponent = instance.diComponent
    }
}