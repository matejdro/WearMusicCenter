package com.matejdro.wearmusiccenter.actions

interface ActionHandler<T : PhoneAction> {
    suspend fun handleAction(action: T)
}
