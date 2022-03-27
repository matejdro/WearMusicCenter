package com.matejdro.wearmusiccenter.di

import com.matejdro.wearmusiccenter.actions.ActionHandler
import com.matejdro.wearmusiccenter.actions.appplay.AppPlayAction
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
}
