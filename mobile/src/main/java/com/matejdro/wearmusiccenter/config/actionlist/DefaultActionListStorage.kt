package com.matejdro.wearmusiccenter.config.actionlist

import android.content.Context
import com.matejdro.wearmusiccenter.actions.PhoneAction

class DefaultActionListStorage(val context: Context) : ActionListStorage {
    override var actions: List<PhoneAction> = emptyList()
    private val diskStorage = DiskActionListStorage(context)

    init {
        diskStorage.loadActions(this)
    }

    override fun commit() {
        //TODO send to watch

        diskStorage.saveActions(actions)
    }

}