package com.matejdro.wearmusiccenter.di

import androidx.lifecycle.lifecycleScope
import com.matejdro.wearmusiccenter.config.ActionConfig
import com.matejdro.wearmusiccenter.view.actionlist.ActionListFragment
import com.matejdro.wearmusiccenter.view.buttonconfig.ButtonConfigFragment
import com.matejdro.wearmusiccenter.view.buttonconfig.GesturePickerFragment
import com.matejdro.wearmusiccenter.view.mainactivity.MainActivity
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.CoroutineScope

@Module
class MainActivityModule {
    // Main Activity does not provide any different config local config - just reuse global config
    @Provides
    @PerContextLifecycle
    @LocalActivityConfig
    fun provideLocalConfig(@GlobalConfig globalConfig: ActionConfig): ActionConfig = globalConfig

    @Provides
    fun provideCoroutineScope(activity: MainActivity): CoroutineScope {
        return activity.lifecycleScope
    }
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
