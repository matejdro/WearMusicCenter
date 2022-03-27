package com.matejdro.wearmusiccenter.actions.volume

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.SelectableAction

class DecreaseVolumeAction : SelectableAction {
    constructor(context : Context) : super(context)
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun retrieveTitle(): String = context.getString(R.string.volume_down)
    override val defaultIcon: Drawable
        get() = AppCompatResources.getDrawable(context, R.drawable.action_volume_down)!!
}
