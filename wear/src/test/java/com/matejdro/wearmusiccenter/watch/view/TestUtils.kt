package com.matejdro.wearmusiccenter.watch.view

import org.robolectric.shadows.ShadowSystemClock

/**
 * Dummy extension of Function2 fixes Roboletric crash
 */
interface Function2<P1, P2, R> : kotlin.Function2<P1, P2, R>

fun advanceTime(durationMs : Int) {
    ShadowSystemClock.sleep(durationMs.toLong())
}
