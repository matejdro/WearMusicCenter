package com.matejdro.wearmusiccenter.watch.view

import android.view.KeyEvent
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_LONG_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.matejdro.wearmusiccenter.watch.util.SystemClockProvider
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StemButtonsManagerTest {
    @Test
    fun testSinglePressWithOthersDisabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = false
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(100)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }

    @Test
    fun testSinglePressWithOthersEnabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(200)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }

    @Test
    fun testSinglePressWithOnlyDoubleEnabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(200)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }

    @Test
    fun testSinglePressWithOnlyLongEnabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = false
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }

    @Test
    fun testSinglePressOutOfBounds() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

    }

    @Test
    fun testRepeatingEvents() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(0L, 0L, KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN, 0))
        advanceTimeBy(50)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(0L, 0L, KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN, 1))
        advanceTimeBy(50)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(0L, 0L, KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN, 2))
        advanceTimeBy(50)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(0L, 0L, KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN, 3))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }


    @Test
    fun testSinglePressSecondButton() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_2, GESTURE_SINGLE_TAP)
    }

    @Test
    fun testDoublePress() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enableDoublePressInAmbient = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_DOUBLE_TAP)
    }

    @Test
    fun testDoublePressWithOnlyItEnabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = false

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_DOUBLE_TAP)
    }


    @Test
    fun testDoublePressSecondButton() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_2] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_2] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        advanceTimeBy(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_2, GESTURE_DOUBLE_TAP)
    }

    /**
     * Test for glitch where single button press in ambient get duplicated after ambient exit
     * several milliseconds after
     */
    @Test
    fun testAmbientGlitch() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onEnterAmbient()

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(25)
        buttonsManager.onExitAmbient()
        advanceTimeBy(18)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)
        advanceTimeBy(5)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(60)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }

    @Test
    fun testAmbientGlitch2() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onEnterAmbient()
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(25)
        buttonsManager.onExitAmbient()
        advanceTimeBy(86)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)
        advanceTimeBy(9)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(11)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }

    @Test
    fun testAmbientGlitch3WithAmbientDoubleClickDisabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enableDoublePressInAmbient = false

        buttonsManager.onEnterAmbient()
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(33)
        buttonsManager.onExitAmbient()
        advanceTimeBy(94)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)
        advanceTimeBy(22)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(16)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }

    @Test
    fun testAmbientGlitch4() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onEnterAmbient()
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(25)
        buttonsManager.onExitAmbient()
        advanceTimeBy(88)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)
        advanceTimeBy(9)
        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(11)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }


    @Test
    fun testFastDoublePress() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(28)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(25)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(28)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_DOUBLE_TAP)
    }

    @Test
    fun testDoublePressFromAmbient() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onEnterAmbient()

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(25)
        buttonsManager.onExitAmbient()
        advanceTimeBy(25)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_DOUBLE_TAP)
    }

    @Test
    fun testFastDoublePressFromAmbient() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        buttonsManager.onEnterAmbient()

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(60)
        buttonsManager.onExitAmbient()
        advanceTimeBy(17)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(63)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(28)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_DOUBLE_TAP)
    }


    @Test
    fun testDoublePressWhenDisabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = false
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        // Whenn double press is disabled, it should produce two single click events.

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(100)

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(50)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        advanceTimeBy(600)

        assertEquals(
                listOf(
                        KeyEvent.KEYCODE_STEM_1 to GESTURE_SINGLE_TAP,
                        KeyEvent.KEYCODE_STEM_1 to GESTURE_SINGLE_TAP
                ),
                listener.invocations
        )
    }


    @Test
    fun testLongPressWithLongPressDisabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = false

        // When long press is disabled, it should revert to single press

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_SINGLE_TAP)
    }


    @Test
    fun testLongPress() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        // This gesture only produces single click for now as long press is not implemented yet

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_LONG_TAP)
    }

    @Test
    fun testLongPressWithOnlyItEnabled() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = false
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true

        // This gesture only produces single click for now as long press is not implemented yet

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_1, KeyEvent(KeyEvent.KEYCODE_STEM_1, KeyEvent.ACTION_DOWN))
        advanceTimeBy(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_1)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_1, GESTURE_LONG_TAP)
    }

    @Test
    fun testLongPressSecondButton() = runBlockingTest {
        val listener = TestFunction2()
        val buttonsManager = StemButtonsManager(listOf(KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2), listener, this, TestSystemClock(this), 300, 500)
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_1] = true
        buttonsManager.enabledDoublePressActions[KeyEvent.KEYCODE_STEM_2] = true
        buttonsManager.enabledLongPressActions[KeyEvent.KEYCODE_STEM_2] = true

        // This gesture only produces single click for now as long press is not implemented yet

        buttonsManager.onKeyDown(KeyEvent.KEYCODE_STEM_2, KeyEvent(KeyEvent.KEYCODE_STEM_2, KeyEvent.ACTION_DOWN))
        advanceTimeBy(1000)
        buttonsManager.onKeyUp(KeyEvent.KEYCODE_STEM_2)

        listener.assertWasCalledWith(KeyEvent.KEYCODE_STEM_2, GESTURE_LONG_TAP)
    }
}

private class TestSystemClock(private val testCoroutineScope: TestCoroutineScope) : SystemClockProvider {
    override fun elapsedRealtime(): Long {
        return testCoroutineScope.currentTime
    }
}

private class TestFunction2 : Function2<Int, Int, Boolean> {
    val invocations: MutableList<Pair<Int, Int>> = ArrayList()
    var returnValue: Boolean = true

    override fun invoke(p1: Int, p2: Int): Boolean {
        invocations += p1 to p2

        return returnValue
    }

    fun assertWasCalledWith(p1: Int, p2: Int) {
        val lastCalled = invocations.lastOrNull() ?: throw AssertionError("Method was never called")

        assertEquals(p1, lastCalled.first)
        assertEquals(p2, lastCalled.second)
    }
}