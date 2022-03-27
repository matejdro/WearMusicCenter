package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R

class OpenMenuAction : SelectableAction {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle)

    override fun retrieveTitle(): String = context.getString(R.string.open_actions_menu)
    override val defaultIcon: Drawable
        get() = AppCompatResources.getDrawable(context, R.drawable.action_open_menu)!!
}
