package com.matejdro.wearmusiccenter.watch.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.appcompat.widget.AppCompatTextView
import android.util.AttributeSet

/**
 * TextView that can be displayed either as a filled text or as an outline.
 */
class OutlineTextView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * Outline width in pixels,
     * equivalent to 1dp by default
     */
    var outlineWidth = context.resources.displayMetrics.density * 1
        set(value) {
            field = value
            invalidate()
        }

    /**
     * When *false*, normal text will be displayed.
     * When *true*, only outline of the text will be displayed.
     */
    var displayTextOutline: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        if (displayTextOutline) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = outlineWidth
        } else {
            paint.style = Paint.Style.FILL
        }

        super.onDraw(canvas)
    }
}
