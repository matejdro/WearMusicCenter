package com.matejdro.wearmusiccenter.watch.model

import android.graphics.Bitmap

data class Notification(val title: String, val description: String?, val background: Bitmap?, val time: Long)
