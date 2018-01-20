package com.matejdro.wearmusiccenter.common.buttonconfig

import android.support.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(GESTURE_SINGLE_TAP.toLong(), GESTURE_DOUBLE_TAP.toLong(), GESTURE_LONG_TAP.toLong())
annotation class ButtonGesture

const val GESTURE_SINGLE_TAP = 0
const val GESTURE_DOUBLE_TAP = 1
const val GESTURE_LONG_TAP = 2

const val NUM_BUTTON_GESTURES = 3
