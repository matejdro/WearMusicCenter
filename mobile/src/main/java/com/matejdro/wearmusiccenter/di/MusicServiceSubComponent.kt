package com.matejdro.wearmusiccenter.di

import com.matejdro.wearmusiccenter.actions.ActionHandler
import com.matejdro.wearmusiccenter.music.MusicService
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(modules = [ActionHandlersModule::class])
abstract class MusicServiceSubComponent {
    @Subcomponent.Factory
    interface Factory {
        fun build(@BindsInstance musicService: MusicService): MusicServiceSubComponent
    }

    abstract fun getActionHandlers(): Map<Class<*>, ActionHandler<*>>
}
