package com.matejdro.wearmusiccenter.actions.volume

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import java.lang.UnsupportedOperationException

class VolumeActionList : PhoneAction {
    constructor(context : Context) : super(context)
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        throw UnsupportedOperationException()
    }

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
            return context.getDrawable(R.drawable.action_volume_up)
        }

}