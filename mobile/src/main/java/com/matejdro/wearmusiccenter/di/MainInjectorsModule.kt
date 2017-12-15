package com.matejdro.wearmusiccenter.di

import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.actionlist.ActionEditorActivity
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerActivity
import com.matejdro.wearmusiccenter.view.mainactivity.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainInjectorsModule {
    @ContributesAndroidInjector
    abstract fun contributeMusicServiceInjector(): MusicService

    @PerContextLifecycle
    @ContributesAndroidInjector(modules = arrayOf(MainActivityModule::class, MainActivityFragments::class))
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector(modules = arrayOf(ActionPickerActivity.Module::class))
    abstract fun contributeActionPickerActivity(): ActionPickerActivity

    @ContributesAndroidInjector()
    abstract fun contributeActionEditorActivity(): ActionEditorActivity
}
