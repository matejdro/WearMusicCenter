package com.matejdro.wearmusiccenter.watch.view

import android.content.Context
import androidx.wear.widget.drawer.WearableDrawerView
import android.util.AttributeSet

/**
 * Drawer view that will disable swipe-to-close function when open
 */
class NoSwipeCloseDrawerView : WearableDrawerView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun canScrollHorizontally(direction: Int): Boolean = true
}
