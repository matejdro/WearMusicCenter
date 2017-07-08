package com.matejdro.wearmusiccenter.actions.playback

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.SelectableAction

class SkipToPrevAction : SelectableAction {
    constructor(context : Context) : super(context)
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        service.currentMediaController?.transportControls?.skipToPrevious()

    }

    override fun getName(): String = context.getString(R.string.action_skip_prev)
    override fun getIcon(): Drawable = context.getDrawable(R.drawable.action_skip_prev)
}