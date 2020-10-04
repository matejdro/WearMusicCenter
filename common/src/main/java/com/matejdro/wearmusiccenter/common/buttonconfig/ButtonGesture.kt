package com.matejdro.wearmusiccenter.common.buttonconfig

import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(GESTURE_SINGLE_TAP, GESTURE_DOUBLE_TAP, GESTURE_LONG_TAP)
annotation class ButtonGesture

const val GESTURE_SINGLE_TAP = 0
const val GESTURE_DOUBLE_TAP = 1
const val GESTURE_LONG_TAP = 2

const val NUM_BUTTON_GESTURES = 3
