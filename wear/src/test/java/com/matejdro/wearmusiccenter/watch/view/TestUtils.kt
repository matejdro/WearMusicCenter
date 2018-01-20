package com.matejdro.wearmusiccenter.watch.view

import kotlinx.coroutines.experimental.delay
import org.robolectric.shadows.ShadowSystemClock

/**
 * Dummy extension of Function2 fixes Roboletric crash
 */
interface Function2<P1, P2, R> : kotlin.Function2<P1, P2, R>

suspend fun advanceTime(durationMs : Int) {
    delay(durationMs)
    ShadowSystemClock.sleep(durationMs.toLong())
}
