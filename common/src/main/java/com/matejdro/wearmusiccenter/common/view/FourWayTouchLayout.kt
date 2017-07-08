package com.matejdro.wearmusiccenter.common.view

import android.view.ViewGroup
import android.widget.FrameLayout
import com.matejdro.common.R
import com.matejdro.wearmusiccenter.common.ScreenQuadrant

class FourWayTouchLayout : FrameLayout, android.view.GestureDetector.OnGestureListener, android.view.GestureDetector.OnDoubleTapListener {
    private val gestureDetector: android.support.v4.view.GestureDetectorCompat

    private var viewSize: Int = 0
    private val quadrantRipples : Array<android.graphics.drawable.Drawable>


    var listener: UserActionListener? = null
    var enabledDoubleTaps = booleanArrayOf(false, false, false, false)

    constructor(context: android.content.Context, attrs: android.util.AttributeSet?, @android.support.annotation.AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        this.gestureDetector = android.support.v4.view.GestureDetectorCompat(context, this)
        gestureDetector.setOnDoubleTapListener(this)

        val rippleColor = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.music_screen_ripple))

        quadrantRipples = Array<android.graphics.drawable.Drawable>(4) { TriangleRippleDrawable(it, rippleColor) }

        post({
            for (i in 0..3) {
                val rippleImageView = createFullScreenImageView()
                rippleImageView.setImageDrawable(quadrantRipples[i])
            }
        })
    }

    constructor(context: android.content.Context) : this(context, null, 0)

    constructor(context: android.content.Context, attrs: android.util.AttributeSet?) : this(context, attrs, 0)

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.actionMasked == android.view.MotionEvent.ACTION_UP || event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) {
            quadrantRipples.forEach { it.state = IntArray(0) }
        }

        return gestureDetector.onTouchEvent(event)
    }


    override fun onDown(e: android.view.MotionEvent): Boolean {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())

        val ripple = quadrantRipples[quadrant]
        ripple.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
        ripple.setHotspot(e.x, e.y)

        return true
    }

    override fun onShowPress(e: android.view.MotionEvent) {
    }

    override fun onSingleTapUp(e: android.view.MotionEvent): Boolean {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())
        if (enabledDoubleTaps[quadrant]) {
            // This method is only handling single taps
            return false
        }

        // Send cancel event to gesture detector to stop it from detecting any further taps
        // as double tap and instead report repeated single taps
        val cancelEvent = android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        gestureDetector.onTouchEvent(cancelEvent)
        cancelEvent.recycle()

        listener?.onSingleTap(quadrant)
        return true
    }

    override fun onScroll(e1: android.view.MotionEvent, e2: android.view.MotionEvent, distanceX: Float, distanceY: Float): Boolean = false

    override fun onLongPress(e: android.view.MotionEvent) = Unit

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        viewSize = Math.max(measuredWidth, measuredHeight)
    }


    private fun createFullScreenImageView(): android.widget.ImageView {
        val imageView = android.widget.ImageView(context)

        val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)

        imageView.layoutParams = params
        addView(imageView)
        return imageView
    }

    private fun getQuadrant(x: Int, y: Int): Int {
        if (x > viewSize / 2) {
            //We are either in top OR Right OR Bottom

            if (y > viewSize / 2) {
                //We are either in Right OR Bottom
                return if (x > y) ScreenQuadrant.RIGHT else ScreenQuadrant.BOTTOM
            } else {
                //We are either in Right OR Top
                return if (x > viewSize - y) ScreenQuadrant.RIGHT else ScreenQuadrant.TOP
            }
        } else {
            //We are either in left OR Right OR Bottom

            if (y > viewSize / 2) {
                //We are either in Left OR Bottom
                return if (viewSize - x > y) ScreenQuadrant.LEFT else ScreenQuadrant.BOTTOM
            } else {
                //We are either in Left OR Top
                return if (x > y) ScreenQuadrant.TOP else ScreenQuadrant.LEFT
            }
        }
    }

    override fun onFling(e1: android.view.MotionEvent, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (Math.abs(velocityY) > Math.abs(velocityX) && velocityY < 0) {
            listener?.onUpwardsSwipe()
            return true
        }

        return false
    }

    override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())
        if (!enabledDoubleTaps[quadrant]) {
            return false
        }

        listener?.onSingleTap(quadrant)

        return true
    }

    override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())
        if (!enabledDoubleTaps[quadrant]) {
            return false
        }

        listener?.onDoubleTap(quadrant)

        return true
    }

    override fun onDoubleTapEvent(e: android.view.MotionEvent): Boolean = false

    interface UserActionListener {
        fun onUpwardsSwipe()
        fun onSingleTap(quadrant: Int)
        fun onDoubleTap(quadrant: Int)
    }
}
