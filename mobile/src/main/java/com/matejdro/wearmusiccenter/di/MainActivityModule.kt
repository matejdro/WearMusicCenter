package com.matejdro.wearmusiccenter.di

import com.matejdro.wearmusiccenter.config.ActionConfig
import com.matejdro.wearmusiccenter.view.actionlist.ActionListFragment
import com.matejdro.wearmusiccenter.view.buttonconfig.ButtonConfigFragment
import com.matejdro.wearmusiccenter.view.buttonconfig.GesturePickerFragment
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector

@Module
class MainActivityModule {
    // Main Activity does not provide any different config local config - just reuse global config
    @Provides
    @PerContextLifecycle
    @LocalActivityConfig
    fun provideLocalConfig(@GlobalConfig globalConfig: ActionConfig): ActionConfig
            = globalConfig
}

@Module
abstract class MainActivityFragments {
    @ContributesAndroidInjector(modules = arrayOf(ButtonConfigFragment.Module::class))
    abstract fun contributeButtonConfigFragment(): ButtonConfigFragment

    @ContributesAndroidInjector
    abstract fun contributeActionListFragment(): ActionListFragment

    @ContributesAndroidInjector
    abstract fun contributeGesturePickerFragment(): GesturePickerFragment
}
