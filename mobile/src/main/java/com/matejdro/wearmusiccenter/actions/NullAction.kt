package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import javax.inject.Inject

class NullAction : PhoneAction {
    constructor(context : Context) : super(context)
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        actionPicker.selectedAction.value = null
    }

    override fun retrieveTitle(): String = context.getString(R.string.no_action)

    override val defaultIcon: Drawable
        get() = AppCompatResources.getDrawable(context, R.drawable.ic_cross_black)!!

    class Handler @Inject constructor() : ActionHandler<NullAction> {
        override suspend fun handleAction(action: NullAction) = Unit
    }
}
