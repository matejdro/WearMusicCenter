package com.matejdro.wearmusiccenter.config.buttons

import android.content.Context
import com.matejdro.wearmusiccenter.actions.NullAction
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo

interface ButtonConfig {
    fun saveButtonAction(buttonInfo: ButtonInfo, action : PhoneAction?)
    fun getScreenAction(buttonInfo: ButtonInfo) : PhoneAction?
    fun getAllActions() : Collection<Map.Entry<ButtonInfo, PhoneAction>>

    fun commit()
}

fun ButtonConfig.getScreenActionFallback(context: Context, buttonInfo: ButtonInfo): PhoneAction {
    val action = getScreenAction(buttonInfo)
    return action ?: NullAction(context)
}