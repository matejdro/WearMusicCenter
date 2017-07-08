package com.matejdro.wearmusiccenter.view.mainactivity

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.matejdro.wearmusiccenter.WearMusicCenter
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import com.matejdro.wearmusiccenter.di.ConfigActivityComponent
import com.matejdro.wearmusiccenter.di.DaggerConfigActivityComponent
import javax.inject.Inject

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val configActivityComponent : ConfigActivityComponent = DaggerConfigActivityComponent
            .builder()
            .appComponent(WearMusicCenter.getAppComponent())
            .build()

    @Inject
    lateinit var watchInfoProvider: WatchInfoProvider

    init {
        configActivityComponent.inject(this)
    }
}