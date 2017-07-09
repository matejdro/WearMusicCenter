package com.matejdro.wearmusiccenter.di

import com.matejdro.wearmusiccenter.view.buttonconfig.ButtonConfigViewModel
import com.matejdro.wearmusiccenter.view.buttonconfig.GesturePickerFragment
import com.matejdro.wearmusiccenter.view.mainactivity.MainActivityViewModel
import dagger.Component

@PerContextLifecycle
@Component(dependencies = arrayOf(AppComponent::class), modules = arrayOf(PerActivityConfigModule::class))
interface ConfigActivityComponent {
    fun inject(viewModel : MainActivityViewModel)
    fun inject(viewModel : ButtonConfigViewModel)
    fun inject(fragment: GesturePickerFragment)

}
