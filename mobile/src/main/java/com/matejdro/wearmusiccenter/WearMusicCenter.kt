package com.matejdro.wearmusiccenter

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.pm.ApplicationInfo
import com.crashlytics.android.Crashlytics
import com.matejdro.wearmusiccenter.di.DaggerAppComponent
import com.matejdro.wearmusiccenter.logging.CrashlyticsExceptionWearHandler
import com.matejdro.wearmusiccenter.logging.TimberCrashlytics
import com.matejdro.wearutils.logging.FileLogger
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import io.fabric.sdk.android.Fabric
import pl.tajchert.exceptionwear.ExceptionDataListenerService
import timber.log.Timber
import javax.inject.Inject


class WearMusicCenter : Application(), HasActivityInjector, HasServiceInjector {
    @Inject
    lateinit var activityInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    override fun onCreate() {
        DaggerAppComponent.builder()
                .application(this)
                .build()
                .inject(this)

        super.onCreate()

        Timber.setAppTag("WearMusicCenter")

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        Timber.plant(Timber.AndroidDebugTree(isDebuggable))

        if (!isDebuggable) {
            Fabric.with(this, Crashlytics())
            Timber.plant(TimberCrashlytics())
            ExceptionDataListenerService.setHandler(CrashlyticsExceptionWearHandler())
        }

        val fileLogger = FileLogger.getInstance(this)
        fileLogger.activate()
        Timber.plant(fileLogger)
    }

    override fun activityInjector(): AndroidInjector<Activity>
            = activityInjector

    override fun serviceInjector(): AndroidInjector<Service>
            = serviceInjector
}