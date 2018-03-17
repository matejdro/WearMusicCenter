package com.matejdro.wearmusiccenter.watch.view

import android.content.Context
import android.graphics.Canvas
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet

/**
 * TextView that draws itself multiple times, causing drawn shadow to be
 * more opaque. This can be used to fake text outline with built-in TextView shadows.
 */
class OutlineTextView : AppCompatTextView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        repeat(5) {
            super.onDraw(canvas)
        }
    }
}