package com.matejdro.wearmusiccenter.di

import android.content.Context
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {
    fun provideContext() :  Context
    fun provideWatchInfoProvider() : WatchInfoProvider
}
