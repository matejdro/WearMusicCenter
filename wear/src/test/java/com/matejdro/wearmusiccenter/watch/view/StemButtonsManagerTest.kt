package com.matejdro.wearmusiccenter.watch.view

import android.view.KeyEvent
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StemButtonsManagerTest {
    @Test
    fun testSinglePressWithOthersDisabled() = runBlocking {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = false
        buttonsManager.enabledLongPressActions[0] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(100)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testSinglePressWithOthersEnabled() = runBlocking {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testDoublePress() = runBlocking {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_DOUBLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    /**
     * Test for glitch where single button press in ambient get duplicated after ambient exit
     * several milliseconds after
     */
    @Test
    fun testAmbientGlitch() = runBlocking {
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
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(5)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(60)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }

    @Test
    fun testDoublePressFromAmbient() = runBlocking {
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
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(600)

        Mockito.verify(listener).invoke(0, GESTURE_DOUBLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testDoublePressWhenDisabled() = runBlocking {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = false
        buttonsManager.enabledLongPressActions[0] = true

        // Whenn double press is disabled, it should produce two single click events.

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        advanceTime(600)

        Mockito.verify(listener, Mockito.times(2)).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testLongPressWithLongPressDisabled() = runBlocking {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = false

        // When long press is disabled, it should revert to single press

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }


    @Test
    fun testLongPress() = runBlocking {
        val listener: Function2<Int, Int, Unit> = mock()
        val buttonsManager = StemButtonsManager(1, listener)
        buttonsManager.enabledDoublePressActions[0] = true
        buttonsManager.enabledLongPressActions[0] = true

        // This gesture only produces single click for now as long press is not implemented yet

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTime(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_UP))

        Mockito.verify(listener).invoke(0, GESTURE_SINGLE_TAP)
        advanceTime(2000)
        Mockito.verifyNoMoreInteractions(listener)
    }
}