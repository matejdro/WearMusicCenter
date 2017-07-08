package com.matejdro.wearmusiccenter.di

import com.matejdro.wearmusiccenter.music.MusicService
import dagger.Component

@PerContextLifecycle
@Component(dependencies = arrayOf(AppComponent::class), modules = arrayOf(StandardConfigModule::class))
interface MusicServiceComponent {
    fun inject(service : MusicService)

}
