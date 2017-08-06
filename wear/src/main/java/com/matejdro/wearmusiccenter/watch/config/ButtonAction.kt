package com.matejdro.wearmusiccenter.watch.config

import android.graphics.drawable.Drawable

class ButtonAction(val key: String, val icon: Drawable?, val title: String? = null) {
    override fun toString(): String {
        return "ButtonAction(key='$key', icon=$icon, title=$title)"
    }
}