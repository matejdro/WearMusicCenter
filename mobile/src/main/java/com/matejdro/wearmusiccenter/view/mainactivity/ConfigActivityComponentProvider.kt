package com.matejdro.wearmusiccenter.view.mainactivity

import com.matejdro.wearmusiccenter.di.ConfigActivityComponent

interface ConfigActivityComponentProvider {
    fun provideConfigActivityComponent() : ConfigActivityComponent
}