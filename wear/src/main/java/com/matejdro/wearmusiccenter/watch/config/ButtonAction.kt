package com.matejdro.wearmusiccenter.watch.config

import android.graphics.Bitmap

class ButtonAction(val key: String, val icon: Bitmap?, val title: String? = null) {
    override fun toString(): String {
        return "ButtonAction(key='$key', icon=$icon, title=$title)"
    }
}