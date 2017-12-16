package com.matejdro.wearmusiccenter.di

import android.app.Application
import android.content.Context
import com.matejdro.wearmusiccenter.config.ActionConfig
import com.matejdro.wearmusiccenter.config.DefaultConfigGenerator
import com.matejdro.wearmusiccenter.config.GlobalActionConfig
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.config.actionlist.GlobalActionList
import com.matejdro.wearmusiccenter.config.buttons.GlobalButtonConfigFactory
import dagger.Lazy
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
                             actionListConfig: Lazy<GlobalActionList>,
                             globalButtonConfigFactory: GlobalButtonConfigFactory): ActionConfig
            = GlobalActionConfig(context,
            watchInfoProvider,
            defaultConfigGenerator,
            actionListConfig,
            globalButtonConfigFactory)
}
