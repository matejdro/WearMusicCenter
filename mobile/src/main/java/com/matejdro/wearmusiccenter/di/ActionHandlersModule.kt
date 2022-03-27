package com.matejdro.wearmusiccenter.di

import com.matejdro.wearmusiccenter.actions.ActionHandler
import com.matejdro.wearmusiccenter.actions.NullAction
import com.matejdro.wearmusiccenter.actions.OpenPlaylistAction
import com.matejdro.wearmusiccenter.actions.appplay.AppPlayAction
import com.matejdro.wearmusiccenter.actions.playback.PauseAction
import com.matejdro.wearmusiccenter.actions.playback.PlayAction
import com.matejdro.wearmusiccenter.actions.playback.ReverseThirtySecondsAction
import com.matejdro.wearmusiccenter.actions.playback.SkipThirtySecondsAction
import com.matejdro.wearmusiccenter.actions.playback.SkipToNextAction
import com.matejdro.wearmusiccenter.actions.playback.SkipToPrevAction
import com.matejdro.wearmusiccenter.actions.tasker.TaskerTaskAction
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

    @Binds
    @IntoMap
    @ClassKey(SkipThirtySecondsAction::class)
    abstract fun bindSkipThirtySecondsActionsHandler(handler: SkipThirtySecondsAction.Handler): ActionHandler<*>

    @Binds
    @IntoMap
    @ClassKey(SkipToNextAction::class)
    abstract fun bindSkipToNextActionsHandler(handler: SkipToNextAction.Handler): ActionHandler<*>

    @Binds
    @IntoMap
    @ClassKey(SkipToPrevAction::class)
    abstract fun bindSkipToPrevActionsHandler(handler: SkipToPrevAction.Handler): ActionHandler<*>

    @Binds
    @IntoMap
    @ClassKey(TaskerTaskAction::class)
    abstract fun bindTaskerTaskActionsHandler(handler: TaskerTaskAction.Handler): ActionHandler<*>

    @Binds
    @IntoMap
    @ClassKey(NullAction::class)
    abstract fun bindNullActionsHandler(handler: NullAction.Handler): ActionHandler<*>

    @Binds
    @IntoMap
    @ClassKey(OpenPlaylistAction::class)
    abstract fun bindOpenPlaylistActionsHandler(handler: NullAction.Handler): ActionHandler<*>
}
