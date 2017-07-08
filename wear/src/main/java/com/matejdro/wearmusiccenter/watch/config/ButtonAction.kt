package com.matejdro.wearmusiccenter.watch.config

import android.graphics.Bitmap

class ButtonAction(val key : String, val icon : Bitmap?) {
    override fun toString(): String {
        return "ButtonAction(key='$key', icon=$icon)"
    }
}