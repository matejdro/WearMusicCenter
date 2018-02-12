package com.matejdro.wearmusiccenter.di

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dagger.Lazy
import javax.inject.Inject

class InjectableViewModelFactory<VM> @Inject constructor(private val viewModel: Lazy<VM>)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return viewModel.get() as T
    }
}