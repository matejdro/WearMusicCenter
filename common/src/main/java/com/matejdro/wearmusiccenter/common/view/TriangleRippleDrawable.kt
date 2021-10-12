package com.matejdro.wearmusiccenter.common.view

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import com.matejdro.wearmusiccenter.common.ScreenQuadrant

class TriangleRippleDrawable(private var quadrant: Int, color: ColorStateList) : RippleDrawable(color, null, ColorDrawable(Color.WHITE)) {
    private val path: Path = Path()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        updatePath(bounds)
    }

    private fun updatePath(bounds: Rect) {
        val width = bounds.width()
        val height = bounds.height()

        path.reset()

        when (quadrant) {
            ScreenQuadrant.LEFT -> {
                path.moveTo(0f, 0f)
                path.lineTo((width / 2).toFloat(), (height / 2).toFloat())
                path.lineTo(0f, height.toFloat())
            }
            ScreenQuadrant.RIGHT -> {
                path.moveTo(width.toFloat(), 0f)
                path.lineTo((width / 2).toFloat(), (height / 2).toFloat())
                path.lineTo(width.toFloat(), height.toFloat())
            }
            ScreenQuadrant.BOTTOM -> {
                path.moveTo(0f, height.toFloat())
                path.lineTo((width / 2).toFloat(), (height / 2).toFloat())
                path.lineTo(width.toFloat(), height.toFloat())
            }
            else -> {
                path.moveTo(0f, 0f)
                path.lineTo((width / 2).toFloat(), (height / 2).toFloat())
                path.lineTo(width.toFloat(), 0f)
            }
        }

        path.close()
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(path)
        super.draw(canvas)
    }
}
