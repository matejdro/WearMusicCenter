package com.matejdro.wearmusiccenter.watch.view

class CircularVolumeBar : android.view.View
{
    private val foregroundPaint: android.graphics.Paint = android.graphics.Paint()
    private val backgroundPaint: android.graphics.Paint

    private val circleBounds = android.graphics.RectF()
    private var viewSize = 0f
    private var minTouchDistanceSquared = 0f
    private var maxTouchDistanceSquared = 0f

    private var touchingCircle = false


    var volumeListener : ((newVolume: Float) -> Unit)? = null

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

        circleBounds.left = foregroundPaint.strokeWidth / 2
        circleBounds.top = foregroundPaint.strokeWidth / 2
    }

    var volume = 0.5f
        set(value) {
            field = value
            invalidate()
        }

    fun incrementVolume(change : Float) {
        updateVolume(Math.min(1f, Math.max(0f, volume + change)))
    }

    private fun updateVolume(newVolume : Float) {
        volume = newVolume
        volumeListener?.invoke(newVolume)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        viewSize = Math.max(measuredWidth, measuredHeight).toFloat()

        val circleSize = viewSize - foregroundPaint.strokeWidth / 2

        circleBounds.right = circleSize
        circleBounds.bottom = circleSize

        minTouchDistanceSquared = viewSize / 2 - foregroundPaint.strokeWidth
        minTouchDistanceSquared *= minTouchDistanceSquared

        maxTouchDistanceSquared = viewSize / 2
        maxTouchDistanceSquared *= maxTouchDistanceSquared
    }

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        if (event == null) {
            touchingCircle = false
            return false
        }

        if (event.actionMasked != android.view.MotionEvent.ACTION_DOWN &&
                (touchingCircle && event.actionMasked != android.view.MotionEvent.ACTION_MOVE)) {
            touchingCircle = false
            return false
        }

        val xFromCenter = event.x - viewSize / 2
        val yFromCenter = event.y - viewSize / 2
        val distFromCenterSquared = xFromCenter * xFromCenter + yFromCenter * yFromCenter

        if (distFromCenterSquared !in minTouchDistanceSquared..maxTouchDistanceSquared) {
            touchingCircle = false
            return false
        }

        touchingCircle = true
        var circlePosition = Math.atan2(xFromCenter.toDouble(), yFromCenter.toDouble())
        if (circlePosition < 0) {
            circlePosition += Math.PI * 2
        }

        circlePosition = Math.PI * 2 - circlePosition
        circlePosition = (circlePosition + Math.PI) % (Math.PI * 2)

        val newVolume = circlePosition / Math.PI / 2
        updateVolume(newVolume.toFloat())

        return true
    }

    override fun onDraw(canvas: android.graphics.Canvas?) {
        super.onDraw(canvas)

        canvas?.drawArc(circleBounds, 0f, 360f, false, backgroundPaint)
        canvas?.drawArc(circleBounds, -90f, volume * 360f, false, foregroundPaint)
    }
}
