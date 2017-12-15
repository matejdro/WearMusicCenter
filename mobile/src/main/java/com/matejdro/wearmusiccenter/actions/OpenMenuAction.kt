package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.music.MusicService
import timber.log.Timber

class OpenMenuAction : SelectableAction {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        Timber.e("Trying to execute non-executable action")
    }

    override fun retrieveTitle(): String = context.getString(R.string.open_actions_menu)
    override val defaultIcon: Drawable
        get() = context.getDrawable(R.drawable.action_open_menu)
}