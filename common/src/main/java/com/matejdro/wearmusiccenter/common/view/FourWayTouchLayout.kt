package com.matejdro.wearmusiccenter.common.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GestureDetectorCompat
import com.matejdro.common.R
import com.matejdro.wearmusiccenter.common.ScreenQuadrant
import kotlin.math.abs
import kotlin.math.max

class FourWayTouchLayout : FrameLayout,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {
    private val gestureDetector = GestureDetectorCompat(context, this)

    private var viewSize: Int = 0
    private val quadrantRipples: Array<Drawable>


    var listener: UserActionListener? = null
    val enabledDoubleTaps = booleanArrayOf(false, false, false, false)
    val enabledLongTaps = booleanArrayOf(false, false, false, false)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        gestureDetector.setOnDoubleTapListener(this)

        val rippleColor = ColorStateList.valueOf(
                ResourcesCompat.getColor(context.resources, R.color.music_screen_ripple, null))

        quadrantRipples = Array(4) { TriangleRippleDrawable(it, rippleColor) }

        post {
            for (i in 0..3) {
                val rippleImageView = createFullScreenImageView()
                rippleImageView.setImageDrawable(quadrantRipples[i])
            }
        }
    }

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            quadrantRipples.forEach { it.state = IntArray(0) }
        }

        return gestureDetector.onTouchEvent(event)
    }


    override fun onDown(e: MotionEvent): Boolean {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())

        val ripple = quadrantRipples[quadrant]
        ripple.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
        ripple.setHotspot(e.x, e.y)

        return true
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())
        if (enabledDoubleTaps[quadrant]) {
            // This method is only handling single taps while double taps are enabled
            return false
        }

        // Send cancel event to gesture detector to stop it from detecting any further taps
        // as double tap and instead report repeated single taps
        val cancelEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        gestureDetector.onTouchEvent(cancelEvent)
        cancelEvent.recycle()

        listener?.onSingleTap(quadrant)
        return true
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        viewSize = max(measuredWidth, measuredHeight)
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

            return if (y > viewSize / 2) {
                //We are either in Right OR Bottom
                if (x > y) ScreenQuadrant.RIGHT else ScreenQuadrant.BOTTOM
            } else {
                //We are either in Right OR Top
                if (x > viewSize - y) ScreenQuadrant.RIGHT else ScreenQuadrant.TOP
            }
        } else {
            //We are either in left OR Right OR Bottom

            return if (y > viewSize / 2) {
                //We are either in Left OR Bottom
                if (viewSize - x > y) ScreenQuadrant.LEFT else ScreenQuadrant.BOTTOM
            } else {
                //We are either in Left OR Top
                if (x > y) ScreenQuadrant.TOP else ScreenQuadrant.LEFT
            }
        }
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (abs(velocityY) > abs(velocityX) && velocityY < 0) {
            listener?.onUpwardsSwipe()
            return true
        }

        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())
        if (!enabledDoubleTaps[quadrant]) {
            return false
        }

        listener?.onSingleTap(quadrant)

        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())
        if (!enabledDoubleTaps[quadrant]) {
            return false
        }

        listener?.onDoubleTap(quadrant)

        return true
    }

    override fun onLongPress(e: MotionEvent) {
        val quadrant = getQuadrant(e.x.toInt(), e.y.toInt())
        if (!enabledLongTaps[quadrant]) {
            return
        }

        listener?.onLongTap(quadrant)
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean = false

    interface UserActionListener {
        fun onUpwardsSwipe()
        fun onSingleTap(quadrant: Int)
        fun onDoubleTap(quadrant: Int)
        fun onLongTap(quadrant: Int)
    }
}
