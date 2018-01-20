package com.matejdro.wearmusiccenter.watch.view

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Message
import android.support.wearable.input.WearableButtons
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import java.lang.ref.WeakReference
import kotlin.math.max

private const val MESSAGE_PRESS_BUTTON = 1
private const val AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_UP = 15
private const val AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_AMBIENT_EXIT = 34

class StemButtonsManager(numStemButtons: Int, private val listener: (buttonIndex: Int, gesture: Int) -> Unit) {
    constructor(context: Context, listener: (buttonIndex: Int, gesture: Int) -> Unit) :
            this(max(0, WearableButtons.getButtonCount(context)), listener)

    private var lastStemPresses = Array<Long>(numStemButtons) { 0 }
    private var lastStemUpEvents = Array<Long>(numStemButtons) { -1 }
    var enabledDoublePressActions = Array(numStemButtons) { false }
    var enabledLongPressActions = Array(numStemButtons) { false }

    private val handler = TimeoutsHandler(WeakReference(this))

    private var lastAmbientExitTime = -1L

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode >= KeyEvent.KEYCODE_STEM_1 && keyCode <= KeyEvent.KEYCODE_STEM_3 && event.repeatCount == 0) {
            val buttonIndex = keyCode - KeyEvent.KEYCODE_STEM_1
            handleStemDown(buttonIndex)

            return true
        }
        return false
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode >= KeyEvent.KEYCODE_STEM_1 && keyCode <= KeyEvent.KEYCODE_STEM_3) {
            val buttonIndex = keyCode - KeyEvent.KEYCODE_STEM_1
            handleStemUp(buttonIndex)
        }


        return false
    }

    fun onEnterAmbient() {
        lastAmbientExitTime = -1
    }

    fun onExitAmbient() {
        lastAmbientExitTime = System.currentTimeMillis()
    }

    private fun handleStemDown(buttonIndex: Int) {
        val now = System.currentTimeMillis()
        if (now - lastAmbientExitTime < AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_AMBIENT_EXIT &&
                now - lastStemUpEvents[buttonIndex] < AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_UP) {
            // Some watches seem to have a bug where clicks are repeated immediately after exiting ambient mode,
            // resulting in double click.

            // Phantom click appears milliseconds after watch exits ambient mode and after up event
            // from prevous click is detected. This makes it unlikely for real human (or even physical button
            // mechanism) to press that fast. Try to detect this fast double click and ignore it.

            return
        }

        handler.removeMessages(MESSAGE_PRESS_BUTTON)

        lastStemUpEvents[buttonIndex] = -1

        if (!enabledDoublePressActions[buttonIndex]) {
            listener.invoke(buttonIndex, GESTURE_SINGLE_TAP)
            return
        }

        val lastPressTime = lastStemPresses[buttonIndex]
        val timeout = ViewConfiguration.getDoubleTapTimeout()

        if (System.currentTimeMillis() - lastPressTime > timeout) {
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_PRESS_BUTTON, buttonIndex, -1),
                    timeout.toLong())
        } else {
            listener.invoke(buttonIndex, GESTURE_DOUBLE_TAP)
        }

        lastStemPresses[buttonIndex] = System.currentTimeMillis()
    }

    private fun handleStemUp(buttonIndex: Int) {
        lastStemUpEvents[buttonIndex] = System.currentTimeMillis()
    }

    private class TimeoutsHandler(val buttonManager: WeakReference<StemButtonsManager>) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_PRESS_BUTTON -> {
                    buttonManager.get()?.listener?.invoke(msg.arg1, GESTURE_SINGLE_TAP)
                }
            }
        }
    }

}