package com.matejdro.wearmusiccenter.watch.util

import android.os.SystemClock

interface SystemClockProvider {
    /**
     * @see android.os.SystemClock.elapsedRealtime
     */
    fun elapsedRealtime(): Long
}

object DefaultSystemClockProvider : SystemClockProvider {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()

}
