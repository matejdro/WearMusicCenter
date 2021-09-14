package com.matejdro.wearmusiccenter.watch.view

import android.annotation.TargetApi
import android.os.Build
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_LONG_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.matejdro.wearmusiccenter.watch.util.DefaultSystemClockProvider
import com.matejdro.wearmusiccenter.watch.util.SystemClockProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MESSAGE_PRESS_BUTTON = 1
private const val MESSAGE_HOLD_BUTTON = 2
private const val AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_UP = 15

@TargetApi(Build.VERSION_CODES.N)
class StemButtonsManager(
        stemButtons: List<Int>,
        listener: (buttonKeyCode: Int, gesture: Int) -> Boolean,
        private val coroutineScope: CoroutineScope,
        systemClock: SystemClockProvider = DefaultSystemClockProvider,
        private val doubleTapTimeout: Int = ViewConfiguration.getDoubleTapTimeout(),
        private val longPressTimeout: Int = ViewConfiguration.getLongPressTimeout()
) {
    var enabledDoublePressActions = stemButtons.associateWith { false }.toMutableMap()
    var enabledLongPressActions = stemButtons.associateWith { false }.toMutableMap()
    var enableDoublePressInAmbient: Boolean = true


    private var buttonHandlers = stemButtons.associateWith { buttonKeyCode ->
        SingleButtonHandler(buttonKeyCode,
                { enabledDoublePressActions[buttonKeyCode] ?: false },
                { enabledLongPressActions[buttonKeyCode] ?: false },
                { enableDoublePressInAmbient },
                listener,
                systemClock,
                coroutineScope,
                doubleTapTimeout,
                longPressTimeout
        )
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount != 0) {
            return false
        }

        return buttonHandlers[keyCode]?.onKeyDown() ?: false
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyUp(keyCode: Int): Boolean {
        return buttonHandlers[keyCode]?.onKeyUp() ?: false
    }

    fun simulateKeyPress(keyCode: Int): Boolean {
        val handled = onKeyDown(keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))

        if (handled) {
            onKeyUp(keyCode)
        }

        return handled
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
                                  private val listener: (buttonKeyCode: Int, gesture: Int) -> Boolean,
                                  private val systemClock: SystemClockProvider,
                                  private val coroutineScope: CoroutineScope,
                                  private val doubleTapTimeout: Int,
                                  private val longPressTimeout: Int
) {

    private var timeoutJob: Job? = null

    private var inAmbientMode = false
    private var lastStemUpEvent = -1L
    private var lastStemPress = -1L
    private var waitingForSecondPress = false
    private var waitingForButtonUp = false
    private var ignoreSecondClick = false


    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyDown(): Boolean {
        val now = systemClock.elapsedRealtime()
        if (lastStemUpEvent > 0 && now - lastStemUpEvent < AMBIENT_REPEAT_HACK_MAX_DIFF_FROM_LAST_UP) {
            // Some watches seem to have a bug where clicks are repeated immediately after exiting ambient mode,
            // resulting in double click.

            // Phantom click appears milliseconds after up event
            // from prevous click. This makes it unlikely for real human (or even physical button
            // mechanism) to press that fast. Try to detect this fast double click and ignore it.

            return false
        }

        lastStemUpEvent = -1

        if (!isDoubleClickEnabled() && !isLongClickEnabled()) {
            return reportGesture(GESTURE_SINGLE_TAP)
        }

        if (isDoubleClickEnabled()) {
            if (inAmbientMode && !isDoubleClickInAmbientEnabled()) {
                ignoreSecondClick = true
            }

            val timeout = doubleTapTimeout

            if (waitingForSecondPress && lastStemPress > 0 && systemClock.elapsedRealtime() - lastStemPress > timeout) {
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
            val timeout = longPressTimeout
            waitingForButtonUp = true
            timeoutJob = coroutineScope.launch {
                delay(timeout.toLong())
                reportGesture(GESTURE_LONG_TAP)
            }
            return true
        }

        return onFirstPress()
    }

    private fun onFirstPress(): Boolean {
        return if (isDoubleClickEnabled()) {
            val timeout = doubleTapTimeout

            timeoutJob = coroutineScope.launch {
                delay(timeout.toLong())
                reportGesture(GESTURE_SINGLE_TAP)
            }

            lastStemPress = systemClock.elapsedRealtime()
            waitingForSecondPress = true
            true
        } else {
            reportGesture(GESTURE_SINGLE_TAP)
        }
    }

    private fun reportGesture(gesture: Int): Boolean {
        val anythingExecuted = listener.invoke(buttonKeyCode, gesture)

        ignoreSecondClick = false
        waitingForSecondPress = false
        waitingForButtonUp = false
        timeoutJob?.cancel()

        return anythingExecuted
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun onKeyUp(): Boolean {
        lastStemUpEvent = systemClock.elapsedRealtime()

        if (waitingForButtonUp) {
            timeoutJob?.cancel()
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
}