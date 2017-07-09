package com.matejdro.wearmusiccenter.actions.playback

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import java.lang.UnsupportedOperationException

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
                SkipToNextAction(context)
        ))
    }

    override fun getName(): String {
        return context.getString(R.string.group_playback_controls)
    }

    override fun retrieveIcon(): Drawable {
        return context.getDrawable(R.drawable.action_play)
    }

}