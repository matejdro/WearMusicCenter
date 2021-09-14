package com.matejdro.wearmusiccenter.common.view

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import com.matejdro.wearmusiccenter.common.ScreenQuadrant
import kotlin.math.max

class TriangleRippleDrawable(private var quadrant: Int, color: ColorStateList) : RippleDrawable(color, null, ColorDrawable(Color.WHITE)) {
    private val path: Path = Path()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        updatePath(bounds)
    }

    private fun updatePath(bounds: Rect) {
        val size = max(bounds.width(), bounds.height())

        path.reset()

        when (quadrant) {
            ScreenQuadrant.LEFT -> {
                path.moveTo(0f, 0f)
                path.lineTo((size / 2).toFloat(), (size / 2).toFloat())
                path.lineTo(0f, size.toFloat())
            }
            ScreenQuadrant.RIGHT -> {
                path.moveTo(size.toFloat(), 0f)
                path.lineTo((size / 2).toFloat(), (size / 2).toFloat())
                path.lineTo(size.toFloat(), size.toFloat())
            }
            ScreenQuadrant.BOTTOM -> {
                path.moveTo(0f, size.toFloat())
                path.lineTo((size / 2).toFloat(), (size / 2).toFloat())
                path.lineTo(size.toFloat(), size.toFloat())
            }
            else -> {
                path.moveTo(0f, 0f)
                path.lineTo((size / 2).toFloat(), (size / 2).toFloat())
                path.lineTo(size.toFloat(), 0f)
            }
        }

        path.close()
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(path)
        super.draw(canvas)
    }
}
