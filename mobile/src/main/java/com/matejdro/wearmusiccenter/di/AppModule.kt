package com.matejdro.wearmusiccenter.di

import android.app.Application
import android.content.Context
import com.matejdro.wearmusiccenter.config.ActionConfigProvider
import com.matejdro.wearmusiccenter.config.DefaultActionConfigProvider
import com.matejdro.wearmusiccenter.config.DefaultConfigGenerator
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.config.actionlist.DefaultActionListStorage
import com.matejdro.wearmusiccenter.config.buttons.DefaultActionConfigStorageFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {
    @Provides
    @Singleton
    fun provideApplicationContext(application: Application): Context = application

    @Provides
    @Singleton
    fun provideWatchStateProvider(context: Context) = WatchInfoProvider(context)

    @Provides
    @Singleton
    @GlobalConfig
    fun provideConfigStorage(context: Context,
                             watchInfoProvider: WatchInfoProvider,
                             defaultConfigGenerator: DefaultConfigGenerator,
                             actionListConfig: DefaultActionListStorage,
                             actionButtonConfig: DefaultActionConfigStorageFactory): ActionConfigProvider
            = DefaultActionConfigProvider(context,
            watchInfoProvider,
            defaultConfigGenerator,
            actionListConfig,
            actionButtonConfig)
}
