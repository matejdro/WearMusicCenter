package com.matejdro.wearmusiccenter.config

import android.content.Context
import com.matejdro.wearmusiccenter.actions.NullAction
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo

interface ActionConfigStorage {
    fun saveButtonAction(buttonInfo: ButtonInfo, action : PhoneAction?)
    fun getScreenAction(buttonInfo: ButtonInfo) : PhoneAction?

    fun commit()
}

fun ActionConfigStorage.getScreenActionFallback(context : Context, buttonInfo: ButtonInfo) : PhoneAction {
    val action = getScreenAction(buttonInfo)

    if (action == null) {
        return NullAction(context)
    } else {
        return action
    }
}