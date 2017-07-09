package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.actions.appplay.AppPlayPickerAction
import com.matejdro.wearmusiccenter.actions.tasker.TaskerTaskPickerAction
import com.matejdro.wearmusiccenter.actions.playback.PlaybackActionList
import com.matejdro.wearmusiccenter.actions.volume.VolumeActionList
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import com.matejdro.wearutils.tasker.TaskerIntent
import java.lang.UnsupportedOperationException

class RootActionList : PhoneAction {
    constructor(context : Context) : super(context)

    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        throw UnsupportedOperationException()
    }

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        val actions = mutableListOf(
                NullAction(context),
                PlaybackActionList(context),
                VolumeActionList(context),
                AppPlayPickerAction(context)
        )

        if (isTaskerInstalled()) {
            actions.add(TaskerTaskPickerAction(context))
        }

        actionPicker.displayedActions.value = actions
    }

    override fun getName(): String {
        throw UnsupportedOperationException()
    }

    override fun getIcon(): Drawable {
        throw UnsupportedOperationException()
    }

    private fun isTaskerInstalled() : Boolean = TaskerIntent.getInstalledTaskerPackage(context) != null
}