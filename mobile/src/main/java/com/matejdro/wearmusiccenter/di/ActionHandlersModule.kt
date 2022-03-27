package com.matejdro.wearmusiccenter.di

import com.matejdro.wearmusiccenter.actions.ActionHandler
import com.matejdro.wearmusiccenter.actions.appplay.AppPlayAction
import com.matejdro.wearmusiccenter.actions.playback.PauseAction
import com.matejdro.wearmusiccenter.actions.playback.PlayAction
import com.matejdro.wearmusiccenter.actions.playback.ReverseThirtySecondsAction
import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
abstract class ActionHandlersModule {
    @Binds
    @IntoMap
    @ClassKey(AppPlayAction::class)
    abstract fun bindAppPlayActionsHandler(handler: AppPlayAction.Handler): ActionHandler<*>

    @Binds
    @IntoMap
    @ClassKey(PauseAction::class)
    abstract fun bindPauseActionsHandler(handler: PauseAction.Handler): ActionHandler<*>

    @Binds
    @IntoMap
    @ClassKey(PlayAction::class)
    abstract fun bindPlayActionsHandler(handler: PlayAction.Handler): ActionHandler<*>

    @Binds
    @IntoMap
    @ClassKey(ReverseThirtySecondsAction::class)
    abstract fun bindReverseThirtySecondsActionsHandler(handler: ReverseThirtySecondsAction.Handler): ActionHandler<*>
}
