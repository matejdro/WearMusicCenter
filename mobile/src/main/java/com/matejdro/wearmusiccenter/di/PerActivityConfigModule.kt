package com.matejdro.wearmusiccenter.di

import android.content.Context
import com.matejdro.wearmusiccenter.config.ActionConfigProvider
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import dagger.Module
import dagger.Provides

@Module
class PerActivityConfigModule {
    @Provides
    @PerContextLifecycle
    @LocalActivityConfig
    fun provideLocalConfig(context: Context,
                           watchInfoProvider: WatchInfoProvider,
                           @GlobalConfig globalConfig: ActionConfigProvider): ActionConfigProvider
            = globalConfig
}
