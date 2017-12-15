package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.actions.appplay.AppPlayPickerAction
import com.matejdro.wearmusiccenter.actions.playback.PlaybackActionList
import com.matejdro.wearmusiccenter.actions.tasker.TaskerTaskPickerAction
import com.matejdro.wearmusiccenter.actions.volume.VolumeActionList
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import com.matejdro.wearutils.tasker.TaskerIntent
import java.lang.UnsupportedOperationException

class RootActionList : PhoneAction {
    private val showNone: Boolean

    constructor(context: Context, showNone: Boolean) : super(context) {
        this.showNone = showNone
    }

    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle) {
        this.showNone = true
    }

    override fun execute(service: MusicService) {
        throw UnsupportedOperationException()
    }

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        val actions = ArrayList<PhoneAction>(20)

        if (showNone) {
            actions.add(NullAction(context))
        }

        actions.addAll(listOf(PlaybackActionList(context),
                VolumeActionList(context),
                AppPlayPickerAction(context),
                OpenMenuAction(context)
        ))

        if (isTaskerInstalled()) {
            actions.add(TaskerTaskPickerAction(context))
        }

        actionPicker.displayedActions.value = actions
    }

    override fun retrieveTitle(): String {
        throw UnsupportedOperationException()
    }

    override val defaultIcon: Drawable
        get() {
            throw UnsupportedOperationException()
        }

    private fun isTaskerInstalled(): Boolean = TaskerIntent.getInstalledTaskerPackage(context) != null
}