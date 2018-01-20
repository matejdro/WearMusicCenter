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

@TargetApi(Build.VERSION_CODES.N)
class StemButtonsManager(numStemButtons: Int, listener: (buttonIndex: Int, gesture: Int) -> Unit) {
    constructor(context: Context, listener: (buttonIndex: Int, gesture: Int) -> Unit) :
            this(max(0, WearableButtons.getButtonCount(context)), listener)

    var enabledDoublePressActions = Array(numStemButtons) { false }
    var enabledLongPressActions = Array(numStemButtons) { false }


    private var buttonHandlers = Array<SingleButtonHandler>(numStemButtons) {
        SingleButtonHandler(it,
                it + KeyEvent.KEYCODE_STEM_1,
                { enabledDoublePressActions[it] },
                { enabledLongPressActions[it] },
                listener)
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode >= KeyEvent.KEYCODE_STEM_1 && keyCode < KeyEvent.KEYCODE_STEM_1 + buttonHandlers.size) {
            val buttonIndex = keyCode - KeyEvent.KEYCODE_STEM_1
            return buttonHandlers[buttonIndex].onKeyDown(keyCode, event)
        }
        return false
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode >= KeyEvent.KEYCODE_STEM_1 && keyCode < KeyEvent.KEYCODE_STEM_1 + buttonHandlers.size) {
            val buttonIndex = keyCode - KeyEvent.KEYCODE_STEM_1
            return buttonHandlers[buttonIndex].onKeyUp(keyCode, event)
        }
        return false
    }

    fun onEnterAmbient() {
        for (it in buttonHandlers) {
            it.onEnterAmbient()
        }
    }

    fun onExitAmbient() {
        for (it in buttonHandlers) {
            it.onExitAmbient()
        }
    }
}

private class SingleButtonHandler(private val buttonIndex: Int,
                                  private val buttonKeyCode: Int,
                                  private val isDoubleClickEnabled: () -> Boolean,
                                  private val isLongClickEnabled: () -> Boolean,
                                  private val listener: (buttonIndex: Int, gesture: Int) -> Unit) {

    private val handler = TimeoutsHandler(WeakReference(this))

    private var lastStemUpEvent = -1L
    private var lastStemPress = -1L
    private var lastAmbientExitTime = -1L


    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastAmbientExitTime < AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_AMBIENT_EXIT &&
                now - lastStemUpEvent < AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_UP) {
            // Some watches seem to have a bug where clicks are repeated immediately after exiting ambient mode,
            // resulting in double click.

            // Phantom click appears milliseconds after watch exits ambient mode and after up event
            // from prevous click is detected. This makes it unlikely for real human (or even physical button
            // mechanism) to press that fast. Try to detect this fast double click and ignore it.

            return false
        }

        handler.removeMessages(MESSAGE_PRESS_BUTTON)

        lastStemUpEvent = -1

        if (!isDoubleClickEnabled()) {
            listener.invoke(buttonIndex, GESTURE_SINGLE_TAP)
            return false
        }

        val lastPressTime = lastStemPress
        val timeout = ViewConfiguration.getDoubleTapTimeout()

        if (System.currentTimeMillis() - lastPressTime > timeout) {
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_PRESS_BUTTON),
                    timeout.toLong())
        } else {
            listener.invoke(buttonIndex, GESTURE_DOUBLE_TAP)
        }

        lastStemPress = System.currentTimeMillis()
        return true
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        lastStemUpEvent = System.currentTimeMillis()

        return false
    }

    fun onEnterAmbient() {
        lastAmbientExitTime = -1
    }

    fun onExitAmbient() {
        lastAmbientExitTime = System.currentTimeMillis()
    }


    private class TimeoutsHandler(val buttonManager: WeakReference<SingleButtonHandler>) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_PRESS_BUTTON -> {
                    buttonManager.get()?.let {
                        it.listener.invoke(it.buttonIndex, GESTURE_SINGLE_TAP)
                    }
                }
            }
        }
    }
}