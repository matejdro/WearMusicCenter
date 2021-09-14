package com.matejdro.wearmusiccenter.actions.playback

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel

class PlaybackActionList : PhoneAction {
    constructor(context : Context) : super(context)
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        throw UnsupportedOperationException()
    }

    override fun onActionPicked(actionPicker: ActionPickerViewModel) {
        actionPicker.updateDisplayedActionsWithBackStack(listOf(
                PlayAction(context),
                PauseAction(context),
                SkipToPrevAction(context),
                SkipToNextAction(context),
                SkipThirtySecondsAction(context),
                ReverseThirtySecondsAction(context)
        ))
    }

    override fun retrieveTitle(): String {
        return context.getString(R.string.group_playback_controls)
    }

    override val defaultIcon: Drawable
        get() {
            return AppCompatResources.getDrawable(context, R.drawable.action_play)!!
        }

}
