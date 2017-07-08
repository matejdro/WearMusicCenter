package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.actions.playback.PlaybackActionList
import com.matejdro.wearmusiccenter.actions.volume.VolumeActionList
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import java.lang.UnsupportedOperationException

class RootActionList : PhoneAction {
    constructor(context : Context) : super(context)

    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        throw UnsupportedOperationException()
    }

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        actionPicker.displayedActions.value = listOf(
                NullAction(context),
                PlaybackActionList(context),
                VolumeActionList(context)
        )
    }

    override fun getName(): String {
        throw UnsupportedOperationException()
    }

    override fun getIcon(): Drawable {
        throw UnsupportedOperationException()
    }
}