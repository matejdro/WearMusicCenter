package com.matejdro.wearmusiccenter.actions.tasker

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.actions.SelectableAction
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearutils.tasker.TaskerIntent

class TaskerTaskAction : SelectableAction {
    companion object {
        const val KEY_TASK_NAME = "TASK_NAME"
    }

    val taskName : String

    constructor(context : Context, taskName : String) : super(context) {
        this.taskName = taskName
    }
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle) {
        this.taskName = bundle.getString(KEY_TASK_NAME)!!
    }


    override fun execute(service: MusicService) {
        val taskerIntent = TaskerIntent(taskName)
        service.sendBroadcast(taskerIntent)
    }

    override fun writeToBundle(bundle: PersistableBundle) {
        super.writeToBundle(bundle)

        bundle.putString(KEY_TASK_NAME, taskName)
    }

    override fun retrieveTitle(): String = taskName
    override val defaultIcon: Drawable
        get() = try {
            context.packageManager.getApplicationIcon(TaskerIntent.getInstalledTaskerPackage(context))
        } catch (ignored: PackageManager.NameNotFoundException) {
            AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
        }

    override fun isEqualToAction(other : PhoneAction) : Boolean {
        other as TaskerTaskAction
        return  super.isEqualToAction(other) && this.taskName == other.taskName
    }
}
