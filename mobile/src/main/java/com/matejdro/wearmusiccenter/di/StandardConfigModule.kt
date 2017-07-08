package com.matejdro.wearmusiccenter.di

import android.content.Context
import com.matejdro.wearmusiccenter.config.*

import dagger.Module
import dagger.Provides

@Module
class StandardConfigModule {
    @Provides
    @PerContextLifecycle
    fun provideConfigStorage(context : Context, watchInfoProvider: WatchInfoProvider) : ActionConfigProvider
            = DefaultActionConfigProvider(context, watchInfoProvider)

}
