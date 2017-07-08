package com.matejdro.wearmusiccenter.common.view

import com.matejdro.wearmusiccenter.common.ScreenQuadrant

class TriangleRippleDrawable(var quadrant: Int, color: android.content.res.ColorStateList) : android.graphics.drawable.RippleDrawable(color, null, android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)) {
    private val path: android.graphics.Path = android.graphics.Path()

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        updatePath(bounds)
    }

    private fun updatePath(bounds: android.graphics.Rect) {
        val size = Math.max(bounds.width(), bounds.height())

        path.reset()

        if (quadrant == ScreenQuadrant.LEFT) {
            path.moveTo(0f, 0f)
            path.lineTo((size / 2).toFloat(), (size / 2).toFloat())
            path.lineTo(0f, size.toFloat())
        } else if (quadrant == ScreenQuadrant.RIGHT) {
            path.moveTo(size.toFloat(), 0f)
            path.lineTo((size / 2).toFloat(), (size / 2).toFloat())
            path.lineTo(size.toFloat(), size.toFloat())
        } else if (quadrant == ScreenQuadrant.BOTTOM) {
            path.moveTo(0f, size.toFloat())
            path.lineTo((size / 2).toFloat(), (size / 2).toFloat())
            path.lineTo(size.toFloat(), size.toFloat())
        } else {
            path.moveTo(0f, 0f)
            path.lineTo((size / 2).toFloat(), (size / 2).toFloat())
            path.lineTo(size.toFloat(), 0f)
        }

        path.close()
    }

    override fun draw(canvas: android.graphics.Canvas) {
        canvas.clipPath(path)
        super.draw(canvas)
    }
}
