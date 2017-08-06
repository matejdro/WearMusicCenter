package com.matejdro.wearmusiccenter.config.actionlist

import com.matejdro.wearmusiccenter.actions.PhoneAction

interface ActionListStorage {
    var actions: List<PhoneAction>

    fun commit()
}