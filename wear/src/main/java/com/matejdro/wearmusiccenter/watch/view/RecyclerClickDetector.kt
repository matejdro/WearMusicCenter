package com.matejdro.wearmusiccenter.watch.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout

/**
 * RecyclerView does not support click detection for whole view. This wrapper view provides that.
 */
class RecyclerClickDetector @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var pressX = 0f
    private var pressY = 0f
    private var prePressed = false

    private val maxAllowedPressMovement = ViewConfiguration.get(context).scaledTouchSlop

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isClickable) {
            return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressX = ev.x
                pressY = ev.y

                postDelayed(pressRunnable, ViewConfiguration.getTapTimeout().toLong())
                prePressed = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (prePressed) {
                    drawableHotspotChanged(ev.x, ev.y)

                    val newY = Math.round(ev.y)
                    val oldYRound = Math.round(pressY)
                    val fingerMovedY = Math.abs(newY - oldYRound)
                    if (fingerMovedY > maxAllowedPressMovement) {
                        // We are scrolling.
                        cancelPress()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(pressRunnable)
                if (prePressed) {
                    performClick()
                    isPressed = true
                    isPressed = false
                    prePressed = false
                    return true
                }
                prePressed = false
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelPress()
            }
        }

        return isPressed
    }

    private fun cancelPress() {
        prePressed = false
        isPressed = false
        removeCallbacks(pressRunnable)
    }

    private val pressRunnable = Runnable {
        drawableHotspotChanged(pressX, pressY)
        isPressed = true
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)

        if (!clickable) {
            cancelPress()
        }
    }

    // Child buttons in RecyclerView should not get pressed when we do
    override fun dispatchSetPressed(pressed: Boolean) = Unit
}