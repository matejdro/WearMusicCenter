package com.matejdro.wearmusiccenter.config

import com.matejdro.wearmusiccenter.actions.PhoneAction

interface ActionListStorage {
    val actions: List<PhoneAction>
}