package com.matejdro.wearmusiccenter.actions.appplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.actions.playback.AppPlayAction
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel

class AppPlayPickerAction : PhoneAction {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        throw UnsupportedOperationException()
    }

    override fun getName(): String = context.getString(R.string.start_playback)
    override fun getIcon(): Drawable = context.getDrawable(R.drawable.ic_apps)

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        val actions = getAllMusicApps(context)
                .map { AppPlayAction(context, it) as PhoneAction }
                .sortedBy { it.getName() }

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
                    .distinctBy { it.packageName }
        }
    }
}