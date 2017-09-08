package com.matejdro.wearmusiccenter.watch.view

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.arch.lifecycle.Observer
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.wear.widget.drawer.WearableDrawerLayout
import android.support.wear.widget.drawer.WearableDrawerView
import android.support.wearable.activity.WearableActivity
import android.support.wearable.input.RotaryEncoder
import android.support.wearable.input.WearableButtons
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.ScreenQuadrant
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.matejdro.wearmusiccenter.common.view.FourWayTouchLayout
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.watch.communication.WatchInfoSender
import com.matejdro.wearmusiccenter.watch.config.WatchActionConfigProvider
import com.matejdro.wearutils.lifecycle.Resource
import java.lang.ref.WeakReference

class MainActivity : WearableActivity(), FourWayTouchLayout.UserActionListener, LifecycleRegistryOwner {
    companion object {
        private const val MESSAGE_HIDE_VOLUME = 0
        private const val MESSAGE_PRESS_BUTTON = 1
        private const val MESSAGE_UPDATE_CLOCK = 2

        private const val VOLUME_BAR_TIMEOUT = 1000L

        @SuppressLint("StaticFieldLeak")
        private var storedViewModel: MusicViewModel? = null
    }

    private lateinit var timeFormat: java.text.DateFormat
    private lateinit var binding: com.matejdro.wearmusiccenter.databinding.ActivityMainBinding
    private lateinit var drawerContentContainer: View
    private lateinit var actionsMenuFragment: ActionsMenuFragment
    private val handler = TimeoutsHandler(WeakReference(this))

    private val lifecycleRegistry = LifecycleRegistry(this)
    lateinit var viewModel: MusicViewModel

    private lateinit var lastStemPresses: Array<Long>
    private lateinit var stemHasDoublePressAction: Array<Boolean>

    //TODO
    val alwaysDisplayTime = true

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        if (storedViewModel == null) {
            storedViewModel = MusicViewModel(application)
        }

        viewModel = storedViewModel as MusicViewModel

        super.onCreate(savedInstanceState)
        binding = android.databinding.DataBindingUtil.setContentView(this, com.matejdro.wearmusiccenter.R.layout.activity_main)
        drawerContentContainer = findViewById(R.id.drawer_content)

        // Hide peek container - we only want full blown drawer without peeks
        val peekContainer: android.view.ViewGroup = binding.drawerLayout.findViewById(
                R.id.ws_drawer_view_peek_container)
        peekContainer.visibility = android.view.View.GONE
        while (peekContainer.childCount > 0) {
            peekContainer.removeViewAt(0)
        }

        val params = peekContainer.layoutParams
        params.width = 0
        params.height = 0
        peekContainer.layoutParams = params

        binding.fourWayTouch.listener = this

        binding.drawerLayout.setDrawerStateCallback(drawerStateCallback)

        timeFormat = android.text.format.DateFormat.getTimeFormat(this)

        // Enables Always-on
        setAmbientEnabled()


        viewModel.albumArt.observe(this, albumArtObserver)
        viewModel.currentConfig.observe(this, configObserver)
        viewModel.volume.observe(this, phoneVolumeListener)
        viewModel.popupVolumeBar.observe(this, volumeBarPopupListener)
        viewModel.closeActionsMenu.observe(this, closeDrawerListener)
        viewModel.openActionsMenu.observe(this, openDrawerListener)

        val numStemButtons = Math.max(0, WearableButtons.getButtonCount(this))
        lastStemPresses = Array<Long>(numStemButtons) { 0 }
        stemHasDoublePressAction = Array(numStemButtons) { false }

        if (alwaysDisplayTime) {
            binding.ambientClock.visibility = View.VISIBLE
            binding.iconTop.visibility = android.view.View.GONE
            handler.sendEmptyMessage(MESSAGE_UPDATE_CLOCK)
        }

        WatchInfoSender(this, true).sendWatchInfoToPhone()
        actionsMenuFragment = fragmentManager.findFragmentById(R.id.drawer_content) as ActionsMenuFragment
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isDestroyed) {
            storedViewModel?.close()
            storedViewModel = null
        }
    }

    override fun onStart() {
        super.onStart()

        if (alwaysDisplayTime) {
            handler.sendEmptyMessage(MESSAGE_UPDATE_CLOCK)
        }
    }

    override fun onStop() {
        super.onStop()

        handler.removeMessages(MESSAGE_UPDATE_CLOCK)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // onStop will trigger when screen turns off (But app stays in foreground)
        // and thus disable data transmission

        // Keep one observer alive as long as app has focus.

        if (hasFocus) {
            viewModel.musicState.observeForever(musicStateObserver)
        } else {
            viewModel.musicState.removeObserver(musicStateObserver)
        }
    }

    override fun onUpdateAmbient() {
        updateClock()
        super.onUpdateAmbient()
    }

    private fun updateClock() {
        binding.ambientClock.text = timeFormat.format(java.util.Date())
    }

    private val musicStateObserver = Observer<Resource<MusicState>> {
        if (it == null || it.status == Resource.Status.LOADING) {
            binding.loadingIndicator.visibility = View.VISIBLE
            return@Observer
        }

        binding.loadingIndicator.visibility = View.GONE

        if (it.status == Resource.Status.SUCCESS && it.data != null) {
            if ((it.data as MusicState).playing) {
                binding.textArtist.text = it.data?.artist
            } else {
                binding.textArtist.text = getString(R.string.playback_stopped)
            }

            binding.textTitle.text = it.data?.title
        } else if (it.status == Resource.Status.ERROR) {
            binding.textArtist.text = getString(R.string.error)
            binding.textTitle.text = it.message
        } else {
            binding.textArtist.text = ""
            binding.textTitle.text = getString(R.string.playback_stopped)
        }

        binding.textArtist.visibility = if (binding.textArtist.text.isEmpty()) View.GONE else View.VISIBLE
    }

    private val albumArtObserver = Observer<Bitmap?> {
        binding.albumArt.setImageBitmap(it)
    }

    private val configObserver = Observer<WatchActionConfigProvider> {
        if (it == null) {
            return@Observer
        }

        val topSingle = it.getAction(ButtonInfo(false, ScreenQuadrant.TOP, GESTURE_SINGLE_TAP))
        val bottomSingle = it.getAction(ButtonInfo(false, ScreenQuadrant.BOTTOM, GESTURE_SINGLE_TAP))
        val leftSingle = it.getAction(ButtonInfo(false, ScreenQuadrant.LEFT, GESTURE_SINGLE_TAP))
        val rightSingle = it.getAction(ButtonInfo(false, ScreenQuadrant.RIGHT, GESTURE_SINGLE_TAP))

        val topDouble = it.getAction(ButtonInfo(false, ScreenQuadrant.TOP, GESTURE_DOUBLE_TAP))
        val bottomDouble = it.getAction(ButtonInfo(false, ScreenQuadrant.BOTTOM, GESTURE_DOUBLE_TAP))
        val leftDouble = it.getAction(ButtonInfo(false, ScreenQuadrant.LEFT, GESTURE_DOUBLE_TAP))
        val rightDouble = it.getAction(ButtonInfo(false, ScreenQuadrant.RIGHT, GESTURE_DOUBLE_TAP))

        binding.iconTop.setImageDrawable(topSingle?.icon)
        binding.iconBottom.setImageDrawable(bottomSingle?.icon)
        binding.iconLeft.setImageDrawable(leftSingle?.icon)
        binding.iconRight.setImageDrawable(rightSingle?.icon)

        binding.fourWayTouch.enabledDoubleTaps = booleanArrayOf(
                leftDouble != null,
                topDouble != null,
                rightDouble != null,
                bottomDouble != null
        )

        val config = it
        stemHasDoublePressAction = Array(lastStemPresses.size) {
            config.isActionActive(ButtonInfo(true, it, GESTURE_DOUBLE_TAP))
        }
    }

    private val phoneVolumeListener = Observer<Float> {
        binding.volumeBar.volume = it!!
    }

    private val volumeBarPopupListener = Observer<Unit> {
        showVolumeBar()
    }

    private val closeDrawerListener = Observer<Unit> {
        closeMenuDrawer()
    }

    fun closeMenuDrawer() {
        binding.actionDrawer.controller.closeDrawer()
    }

    private val openDrawerListener = Observer<Unit> {
        openMenuDrawer()
    }

    fun openMenuDrawer() {
        binding.actionDrawer.controller.openDrawer()
    }

    private val drawerStateCallback = object : WearableDrawerLayout.DrawerStateCallback() {
        override fun onDrawerClosed(layout: WearableDrawerLayout, drawerView: WearableDrawerView) {
            binding.fourWayTouch.requestFocus()
            actionsMenuFragment.scrollToTop()
        }

        override fun onDrawerOpened(layout: WearableDrawerLayout, drawerView: WearableDrawerView) {
            drawerContentContainer.requestFocus()
        }
    }

    override fun onEnterAmbient(ambientDetails: android.os.Bundle?) {
        binding.ambientClock.visibility = android.view.View.VISIBLE

        handler.removeMessages(MESSAGE_UPDATE_CLOCK)
        updateClock()

        binding.iconTop.visibility = android.view.View.GONE
        binding.iconBottom.visibility = android.view.View.GONE
        binding.iconLeft.visibility = android.view.View.GONE
        binding.iconRight.visibility = android.view.View.GONE

        binding.albumArt.visibility = android.view.View.GONE
        binding.volumeBar.visibility = android.view.View.GONE
        binding.loadingIndicator.visibility = View.GONE

        binding.root.background = ColorDrawable(Color.BLACK)

        binding.actionDrawer.controller.closeDrawer()
        super.onEnterAmbient(ambientDetails)
    }

    override fun onExitAmbient() {
        if (alwaysDisplayTime) {

            handler.sendEmptyMessage(MESSAGE_UPDATE_CLOCK)
        } else {
            binding.ambientClock.visibility = android.view.View.GONE
            binding.iconTop.visibility = android.view.View.VISIBLE
        }

        binding.iconBottom.visibility = android.view.View.VISIBLE
        binding.iconLeft.visibility = android.view.View.VISIBLE
        binding.iconRight.visibility = android.view.View.VISIBLE

        binding.albumArt.visibility = android.view.View.VISIBLE

        binding.root.background = null

        if (viewModel.musicState.value == null || (viewModel.musicState.value as Resource<MusicState>).status == Resource.Status.LOADING) {
            binding.loadingIndicator.visibility = View.VISIBLE
        }

        super.onExitAmbient()
    }

    override fun onGenericMotionEvent(ev: android.view.MotionEvent): Boolean {
        if (binding.actionDrawer.isOpened) {
            return false
        }

        if (ev.action == android.view.MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(ev)) {
            val delta = -RotaryEncoder.getRotaryAxisValue(ev) * RotaryEncoder.getScaledScrollFactor(this)

            showVolumeBar()
            binding.volumeBar.incrementVolume(delta * 0.0025f)
            viewModel.updateVolume(binding.volumeBar.volume)


            return true
        }

        return super.onGenericMotionEvent(ev)
    }

    private fun handleStemDown(buttonIndex: Int) {
        handler.removeMessages(MESSAGE_PRESS_BUTTON)

        if (!stemHasDoublePressAction[buttonIndex]) {
            viewModel.executeAction(ButtonInfo(true, buttonIndex, GESTURE_SINGLE_TAP))
            return
        }

        val lastPressTime = lastStemPresses[buttonIndex]
        val timeout = ViewConfiguration.getDoubleTapTimeout()

        if (System.currentTimeMillis() - lastPressTime > timeout) {
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_PRESS_BUTTON, buttonIndex),
                    timeout.toLong())
        } else {
            viewModel.executeAction(ButtonInfo(true, buttonIndex, GESTURE_DOUBLE_TAP))
        }

        lastStemPresses[buttonIndex] = System.currentTimeMillis()
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return super.onKeyDown(keyCode, event)
        }

        if (binding.actionDrawer.isOpened) {
            return actionsMenuFragment.onKeyDown(keyCode, event)
        }

        if (keyCode >= KeyEvent.KEYCODE_STEM_1 && keyCode <= KeyEvent.KEYCODE_STEM_3) {
            val buttonIndex = keyCode - KeyEvent.KEYCODE_STEM_1
            handleStemDown(buttonIndex)

            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun showVolumeBar() {
        binding.volumeBar.visibility = android.view.View.VISIBLE

        handler.removeMessages(MESSAGE_HIDE_VOLUME)
        handler.sendEmptyMessageDelayed(MESSAGE_HIDE_VOLUME, VOLUME_BAR_TIMEOUT)
    }

    override fun onUpwardsSwipe() {
        timber.log.Timber.d("UpwardsSwipe")

        binding.actionDrawer.controller.openDrawer()
    }

    override fun onSingleTap(quadrant: Int) {
        viewModel.executeAction(ButtonInfo(false, quadrant, GESTURE_SINGLE_TAP))
    }

    override fun onDoubleTap(quadrant: Int) {
        viewModel.executeAction(ButtonInfo(false, quadrant, GESTURE_DOUBLE_TAP))
    }

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry

    private class TimeoutsHandler(val activity: java.lang.ref.WeakReference<MainActivity>) : android.os.Handler() {
        override fun dispatchMessage(msg: android.os.Message?) {
            if (msg?.what == MESSAGE_HIDE_VOLUME) {
                activity.get()?.binding?.volumeBar?.visibility = android.view.View.GONE
            } else if (msg?.what == MESSAGE_PRESS_BUTTON) {
                activity.get()?.viewModel?.executeAction(ButtonInfo(true, msg.arg1, GESTURE_SINGLE_TAP))
            } else if (msg?.what == MESSAGE_UPDATE_CLOCK) {
                removeMessages(MESSAGE_UPDATE_CLOCK)

                val activity = activity.get() ?: return

                activity.updateClock()

                if (!activity.isAmbient && activity.alwaysDisplayTime) {
                    sendEmptyMessageDelayed(MESSAGE_UPDATE_CLOCK, 60_000)
                }
            }

        }
    }
}