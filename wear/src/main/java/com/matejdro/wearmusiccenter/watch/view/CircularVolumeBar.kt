package com.matejdro.wearmusiccenter.watch.view

import kotlin.math.max
import kotlin.math.min

class CircularVolumeBar : android.view.View {
    private val foregroundPaint: android.graphics.Paint = android.graphics.Paint()
    private val backgroundPaint: android.graphics.Paint

    private val circleBounds = android.graphics.RectF()

    constructor(context: android.content.Context?) : this(context, null)
    constructor(context: android.content.Context?, attrs: android.util.AttributeSet?) : this(context, attrs, 0)
    constructor(context: android.content.Context?, attrs: android.util.AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: android.content.Context?, attrs: android.util.AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        foregroundPaint.style = android.graphics.Paint.Style.STROKE
        foregroundPaint.strokeWidth = resources.getDimension(com.matejdro.wearmusiccenter.R.dimen.music_screen_volume_bar_width)
        foregroundPaint.strokeCap = android.graphics.Paint.Cap.ROUND
        foregroundPaint.color = resources.getColor(com.matejdro.wearmusiccenter.R.color.music_screen_volume_bar_foreground_color, null)
        foregroundPaint.isAntiAlias = true

        backgroundPaint = android.graphics.Paint(foregroundPaint)
        backgroundPaint.color = resources.getColor(com.matejdro.wearmusiccenter.R.color.music_screen_volume_bar_background_color, null)
    }

    var volume = 0.5f
        set(value) {
            field = value
            invalidate()
        }

    fun incrementVolume(change : Float) {
        updateVolume(min(1f, max(0f, volume + change)))
    }

    private fun updateVolume(newVolume : Float) {
        volume = newVolume
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val viewSize = min(measuredWidth, measuredHeight).toFloat()

        val circleStroke = foregroundPaint.strokeWidth / 2
        val circleSize = viewSize - foregroundPaint.strokeWidth

        val horizontalMargin = measuredWidth - viewSize
        val verticalMargin = measuredHeight - viewSize

        circleBounds.left = circleStroke + horizontalMargin / 2
        circleBounds.top = circleStroke + verticalMargin / 2

        circleBounds.right = circleBounds.left + circleSize
        circleBounds.bottom = circleBounds.top + circleSize
    }

    override fun onDraw(canvas: android.graphics.Canvas?) {
        super.onDraw(canvas)

        canvas?.drawArc(circleBounds, 0f, 360f, false, backgroundPaint)
        canvas?.drawArc(circleBounds, -90f, volume * 360f, false, foregroundPaint)
    }
}
