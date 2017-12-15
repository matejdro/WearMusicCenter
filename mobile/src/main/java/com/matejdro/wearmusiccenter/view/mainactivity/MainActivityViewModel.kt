package com.matejdro.wearmusiccenter.view.mainactivity

import android.arch.lifecycle.ViewModel
import com.kakai.android.autoviewmodelfactory.annotations.AutoViewModelFactory
import com.matejdro.wearmusiccenter.config.WatchInfoProvider

@AutoViewModelFactory
class MainActivityViewModel(val watchInfoProvider: WatchInfoProvider) : ViewModel()