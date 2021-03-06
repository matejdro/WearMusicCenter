package com.matejdro.wearmusiccenter.view.mainactivity

import androidx.lifecycle.ViewModel
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import javax.inject.Inject

class MainActivityViewModel @Inject constructor(val watchInfoProvider: WatchInfoProvider) : ViewModel()
