package com.matejdro.wearmusiccenter.watch.view

import android.view.KeyEvent
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_LONG_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StemButtonsManagerTest {
    @Test
    fun testSinglePressWithOthersDisabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = false
        buttonsManager.enabledLongPressActions[0] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(100)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testSinglePressWithOthersEnabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(200)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testSinglePressWithOnlyDoubleEnabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(200)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testSinglePressWithOnlyLongEnabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = false
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testSinglePressOutOfBounds() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testRepeatingEvents() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(0L, 0L, KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN, 0))
        advanceTime(50)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(0L, 0L, KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN, 1))
        advanceTime(50)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(0L, 0L, KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN, 2))
        advanceTime(50)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(0L, 0L, KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN, 3))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testSinglePressSecondButton() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(2, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        advanceTime(600)

        Mockito.verify(listener).invoke(1, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testDoublePress() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true
        buttonsManager.enableDoublePressInAmbient = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_DOUBLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testDoublePressWithOnlyItEnabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_DOUBLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testDoublePressSecondButton() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(2, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true
        buttonsManager.enabledDoublePressActions[1] = true
        buttonsManager.enabledLongPressActions[1] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        advanceTime(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        advanceTime(600)

        Mockito.verify(listener).invoke(1, GESTURE_DOUBLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    /**
     * Test for glitch where single button press in ambient get duplicated after ambient exit
     * several milliseconds after
     */
    @Test
    fun testAmbientGlitch() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onEnterAmbient()
        advanceTime(2000)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(25)
        buttonsManager.onExitAmbient()
        advanceTime(18)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)
        advanceTime(5)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(60)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testAmbientGlitch2() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onEnterAmbient()
        advanceTime(2000)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(25)
        buttonsManager.onExitAmbient()
        advanceTime(86)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)
        advanceTime(9)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(11)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testAmbientGlitch3WithAmbientDoubleClickDisabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true
        buttonsManager.enableDoublePressInAmbient = false

        buttonsManager.onEnterAmbient()
        advanceTime(2000)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(33)
        buttonsManager.onExitAmbient()
        advanceTime(94)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)
        advanceTime(22)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(16)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testAmbientGlitch4() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onEnterAmbient()
        advanceTime(2000)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(25)
        buttonsManager.onExitAmbient()
        advanceTime(88)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)
        advanceTime(9)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(11)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testFastDoublePress() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(28)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(25)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(28)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_DOUBLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testDoublePressFromAmbient() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onEnterAmbient()
        advanceTime(2000)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(25)
        buttonsManager.onExitAmbient()
        advanceTime(25)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_DOUBLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testFastDoublePressFromAmbient() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onEnterAmbient()
        advanceTime(2000)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(60)
        buttonsManager.onExitAmbient()
        advanceTime(17)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(63)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(28)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_DOUBLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testDoublePressWhenDisabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = false
        buttonsManager.enabledLongPressActions[0] = true

        // Whenn double press is disabled, it should produce two single click events.

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTime(600)

        Mockito.verify(listener, Mockito.times(2)).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testLongPressWithLongPressDisabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = false

        // When long press is disabled, it should revert to single press

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testLongPress() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        // This gesture only produces single click for now as long press is not implemented yet

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        Mockito.verify(listener).invoke(0, GESTURE_LONG_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testLongPressWithOnlyItEnabled() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = false
        buttonsManager.enabledLongPressActions[0] = true

        // This gesture only produces single click for now as long press is not implemented yet

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        Mockito.verify(listener).invoke(0, GESTURE_LONG_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testLongPressSecondButton() {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(2, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true
        buttonsManager.enabledDoublePressActions[1] = true
        buttonsManager.enabledLongPressActions[1] = true

        // This gesture only produces single click for now as long press is not implemented yet

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTime(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        Mockito.verify(listener).invoke(1, GESTURE_LONG_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }
}