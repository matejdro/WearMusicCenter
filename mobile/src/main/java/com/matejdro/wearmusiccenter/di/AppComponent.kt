package com.matejdro.wearmusiccenter.di

import android.content.Context
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.config.ActionConfigProvider
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.actionlist.ActionEditorActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {
    fun provideContext() :  Context
    fun provideWatchInfoProvider() : WatchInfoProvider

    @GlobalConfig
    fun provideConfigStorage() : ActionConfigProvider

    fun inject(phoneAction: PhoneAction)
    fun inject(service: MusicService)
    fun inject(actionEditorActivity: ActionEditorActivity)
}
