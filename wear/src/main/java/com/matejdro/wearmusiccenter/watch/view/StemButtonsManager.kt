package com.matejdro.wearmusiccenter.watch.view

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.support.wearable.input.WearableButtons
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_LONG_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import java.lang.ref.WeakReference

private const val MESSAGE_PRESS_BUTTON = 1
private const val MESSAGE_HOLD_BUTTON = 2
private const val AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_UP = 15

@TargetApi(Build.VERSION_CODES.N)
class StemButtonsManager(stemButtons: List<Int>, listener: (buttonKeyCode: Int, gesture: Int) -> Unit) {
    constructor(context: Context, listener: (buttonIndex: Int, gesture: Int) -> Unit) :
            this(List(WearableButtons.getButtonCount(context)) { KeyEvent.KEYCODE_STEM_1 + it }, listener)

    var enabledDoublePressActions = stemButtons.associateWith { false }.toMutableMap()
    var enabledLongPressActions = stemButtons.associateWith { false }.toMutableMap()
    var enableDoublePressInAmbient: Boolean = true


    private var buttonHandlers = stemButtons.associateWith { buttonKeyCode ->
        SingleButtonHandler(buttonKeyCode,
                { enabledDoublePressActions[buttonKeyCode] ?: false },
                { enabledLongPressActions[buttonKeyCode] ?: false },
                { enableDoublePressInAmbient },
                listener)
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount != 0) {
            return false
        }

        return buttonHandlers[keyCode].also { println("Handler $it") }?.onKeyDown() ?: false
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyUp(keyCode: Int): Boolean {
        return buttonHandlers[keyCode]?.onKeyUp() ?: false
    }

    fun onEnterAmbient() {
        for (it in buttonHandlers.values) {
            it.onEnterAmbient()
        }
    }

    fun onExitAmbient() {
        for (it in buttonHandlers.values) {
            it.onExitAmbient()
        }
    }
}

private class SingleButtonHandler(private val buttonKeyCode: Int,
                                  private val isDoubleClickEnabled: () -> Boolean,
                                  private val isLongClickEnabled: () -> Boolean,
                                  private val isDoubleClickInAmbientEnabled: () -> Boolean,
                                  private val listener: (buttonKeyCode: Int, gesture: Int) -> Unit) {

    private val handler = TimeoutsHandler(WeakReference(this))

    private var inAmbientMode = false
    private var lastStemUpEvent = -1L
    private var lastStemPress = -1L
    private var waitingForSecondPress = false
    private var waitingForButtonUp = false
    private var ignoreSecondClick = false


    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyDown(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastStemUpEvent < AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_UP) {
            // Some watches seem to have a bug where clicks are repeated immediately after exiting ambient mode,
            // resulting in double click.

            // Phantom click appears milliseconds after up event
            // from prevous click. This makes it unlikely for real human (or even physical button
            // mechanism) to press that fast. Try to detect this fast double click and ignore it.

            return false
        }

        lastStemUpEvent = -1

        if (!isDoubleClickEnabled() && !isLongClickEnabled()) {
            reportGesture(GESTURE_SINGLE_TAP)
            return true
        }

        if (isDoubleClickEnabled()) {
            if (inAmbientMode && !isDoubleClickInAmbientEnabled()) {
                ignoreSecondClick = true
            }

            val timeout = ViewConfiguration.getDoubleTapTimeout()

            if (waitingForSecondPress && SystemClock.elapsedRealtime() - lastStemPress > timeout) {
                waitingForSecondPress = false
            }

            if (waitingForSecondPress && ignoreSecondClick) {
                waitingForSecondPress = false
                ignoreSecondClick = false
                return false
            }

            if (waitingForSecondPress) {
                reportGesture(GESTURE_DOUBLE_TAP)
                return true
            }
        }

        if (isLongClickEnabled()) {
            val timeout = ViewConfiguration.getLongPressTimeout()
            waitingForButtonUp = true
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_HOLD_BUTTON), timeout.toLong())
            return true
        }

        onFirstPress()
        return true
    }

    private fun onFirstPress() {
        if (isDoubleClickEnabled()) {
            val timeout = ViewConfiguration.getDoubleTapTimeout()

            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_PRESS_BUTTON), timeout.toLong())

            lastStemPress = SystemClock.elapsedRealtime()
            waitingForSecondPress = true

        } else {
            reportGesture(GESTURE_SINGLE_TAP)
        }
    }

    private fun reportGesture(gesture: Int) {
        listener.invoke(buttonKeyCode, gesture)

        ignoreSecondClick = false
        waitingForSecondPress = false
        waitingForButtonUp = false
        handler.removeCallbacksAndMessages(null)
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyUp(): Boolean {
        lastStemUpEvent = SystemClock.elapsedRealtime()

        if (waitingForButtonUp) {
            handler.removeMessages(MESSAGE_HOLD_BUTTON)
            waitingForButtonUp = false
            onFirstPress()
            return true
        }
        return false
    }

    fun onEnterAmbient() {
        inAmbientMode = true
    }

    fun onExitAmbient() {
        inAmbientMode = false
    }


    private class TimeoutsHandler(val buttonManager: WeakReference<SingleButtonHandler>) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_PRESS_BUTTON -> {
                    buttonManager.get()?.reportGesture(GESTURE_SINGLE_TAP)
                }
                MESSAGE_HOLD_BUTTON -> {
                    buttonManager.get()?.reportGesture(GESTURE_LONG_TAP)
                }
            }
        }
    }
}