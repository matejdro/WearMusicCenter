package com.matejdro.wearmusiccenter.di

import android.app.Application
import android.content.Context
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val application: Application) {
    @Provides
    @Singleton
    fun provideApplicationContext() : Context = application

    @Provides
    @Singleton
    fun provideWatchStateProvider(context : Context) = WatchInfoProvider(context)
}
