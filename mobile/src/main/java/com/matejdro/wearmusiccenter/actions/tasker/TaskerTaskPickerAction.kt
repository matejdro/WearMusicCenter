package com.matejdro.wearmusiccenter.actions.tasker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.ActivityResultReceiver
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import com.matejdro.wearutils.tasker.TaskerIntent

class TaskerTaskPickerAction : PhoneAction, ActivityResultReceiver {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle)

    private var prevActionPicker: ActionPickerViewModel? = null

    override fun execute(service: MusicService) {
        throw UnsupportedOperationException()
    }


    override fun retrieveTitle(): String = context.getString(R.string.tasker_task)
    override val defaultIcon: Drawable
        get() {
            return try {
                context.packageManager.getApplicationIcon(TaskerIntent.TASKER_PACKAGE_MARKET)
            } catch (ignored: PackageManager.NameNotFoundException) {
                AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
            }
        }

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        prevActionPicker = actionPicker
        actionPicker.startActivityForResult(TaskerIntent.getTaskSelectIntent(), this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        val taskName = data?.dataString ?: return

        val taskerAction = TaskerTaskAction(context, taskName)
        prevActionPicker?.selectedAction?.value = taskerAction
    }
}
