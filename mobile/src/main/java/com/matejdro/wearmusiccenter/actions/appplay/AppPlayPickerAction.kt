package com.matejdro.wearmusiccenter.actions.appplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel

class AppPlayPickerAction : PhoneAction {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle)

    override fun retrieveTitle(): String = context.getString(R.string.start_playback)
    override val defaultIcon: Drawable
        get() = AppCompatResources.getDrawable(context, R.drawable.ic_apps)!!

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        val actions = getAllMusicApps(context)
                .map { AppPlayAction(context, it) }
                .sortedBy { it.title }

        actionPicker.updateDisplayedActionsWithBackStack(actions)
    }

    companion object {
        fun getAllMusicApps(context: Context): List<ComponentName> {
            val packageManager = context.packageManager

            val targetIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
            return packageManager
                    .queryBroadcastReceivers(targetIntent, 0)
                    .map {
                        val activityInfo = it.activityInfo
                        ComponentName(activityInfo.packageName, activityInfo.name)
                    }
                    .groupBy { it.packageName }
                    .flatMap { it.value }
        }
    }
}
