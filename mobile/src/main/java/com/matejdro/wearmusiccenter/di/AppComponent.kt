package com.matejdro.wearmusiccenter.di

import android.app.Application
import com.matejdro.wearmusiccenter.WearMusicCenter
import com.matejdro.wearmusiccenter.actions.PhoneAction
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidInjectionModule::class, AppModule::class, MainInjectorsModule::class])
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }


    fun inject(wearMusicCenter: WearMusicCenter)
    fun inject(phoneAction: PhoneAction)
}
