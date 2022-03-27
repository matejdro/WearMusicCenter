package com.matejdro.wearmusiccenter.actions.volume

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel

class VolumeActionList : PhoneAction {
    constructor(context : Context) : super(context)
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        actionPicker.updateDisplayedActionsWithBackStack(listOf(
                IncreaseVolumeAction(context),
                DecreaseVolumeAction(context)
        ))
    }

    override fun retrieveTitle(): String {
        return context.getString(R.string.group_volume_controls)
    }

    override val defaultIcon: Drawable
        get() {
            return AppCompatResources.getDrawable(context, R.drawable.action_volume_up)!!
        }

}
