package com.matejdro.wearmusiccenter.actions.volume

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.SelectableAction

class DecreaseVolumeAction : SelectableAction {
    constructor(context : Context) : super(context)
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {

    }

    override fun getName(): String = context.getString(R.string.volume_down)
    override fun getIcon(): Drawable = context.getDrawable(R.drawable.action_volume_down)
}