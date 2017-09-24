package com.matejdro.wearmusiccenter

import android.app.Application
import android.content.pm.ApplicationInfo
import com.crashlytics.android.Crashlytics
import com.matejdro.wearmusiccenter.di.AppComponent
import com.matejdro.wearmusiccenter.di.AppModule
import com.matejdro.wearmusiccenter.di.DaggerAppComponent
import com.matejdro.wearmusiccenter.logging.CrashlyticsExceptionWearHandler
import com.matejdro.wearmusiccenter.logging.TimberCrashlytics
import io.fabric.sdk.android.Fabric
import pl.tajchert.exceptionwear.ExceptionDataListenerService
import timber.log.Timber


class WearMusicCenter : Application() {
    private lateinit var diComponent : AppComponent
    override fun onCreate() {
        super.onCreate()

        Timber.setAppTag("WearMusicCenter")

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        Timber.plant(Timber.AndroidDebugTree(isDebuggable))

        if (!isDebuggable) {
            Fabric.with(this, Crashlytics())
            Timber.plant(TimberCrashlytics())
            ExceptionDataListenerService.setHandler(CrashlyticsExceptionWearHandler())
        }

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