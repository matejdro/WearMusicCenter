package com.matejdro.wearmusiccenter.view.actionconfigs

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.matejdro.wearmusiccenter.actions.PhoneAction

abstract class ActionConfigFragment<T : PhoneAction> : Fragment {
    constructor(contentLayoutId: Int) : super(contentLayoutId)
    constructor() : super()

    abstract fun save(action: T)

    fun load(action: T) {
        lifecycleScope.launchWhenStarted {
            onLoad(action)
        }
    }

    abstract fun onLoad(action: T)
}
