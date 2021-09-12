package com.matejdro.wearmusiccenter.watch.view

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.preference.PreferenceManager
import android.support.wearable.input.RotaryEncoder
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.wear.ambient.AmbientMode
import androidx.wear.widget.drawer.WearableDrawerLayout
import androidx.wear.widget.drawer.WearableDrawerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.MiscPreferences
import com.matejdro.wearmusiccenter.common.ScreenQuadrant
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_LONG_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.matejdro.wearmusiccenter.common.view.FourWayTouchLayout
import com.matejdro.wearmusiccenter.proto.MusicState
import com.matejdro.wearmusiccenter.watch.communication.CustomListWithBitmaps
import com.matejdro.wearmusiccenter.watch.communication.WatchInfoSender
import com.matejdro.wearmusiccenter.watch.config.WatchActionConfigProvider
import com.matejdro.wearmusiccenter.watch.model.Notification
import com.matejdro.wearutils.companionnotice.WearCompanionWatchActivity
import com.matejdro.wearutils.lifecycle.Resource
import com.matejdro.wearutils.miscutils.VibratorCompat
import com.matejdro.wearutils.preferences.definition.Preferences
import timber.log.Timber
import java.lang.ref.WeakReference

// LifecycleRegistryOwner must be used, because there is no alternative for non-compat activities
class MainActivity : WearCompanionWatchActivity(),
        FourWayTouchLayout.UserActionListener,
        AmbientMode.AmbientCallbackProvider {

    companion object {
        private const val MESSAGE_HIDE_VOLUME = 0
        private const val MESSAGE_UPDATE_CLOCK = 2
        private const val MESSAGE_DISMISS_NOTIFICATION = 3

        private const val VOLUME_BAR_TIMEOUT = 1000L
    }

    private lateinit var timeFormat: java.text.DateFormat
    private lateinit var binding: com.matejdro.wearmusiccenter.databinding.ActivityMainBinding
    private lateinit var drawerContentContainer: View
    private lateinit var actionsMenuFragment: ActionsMenuFragment
    private lateinit var vibrator: Vibrator
    private lateinit var ambientController: AmbientMode.AmbientController
    private lateinit var stemButtonsManager: StemButtonsManager
    private val handler = TimeoutsHandler(WeakReference(this))

    private lateinit var preferences: SharedPreferences

    lateinit var viewModel: MusicViewModel

    private var rotatingInputDisabledUntil = 0L

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this)[MusicViewModel::class.java]

        binding = androidx.databinding.DataBindingUtil.setContentView(this, com.matejdro.wearmusiccenter.R.layout.activity_main)
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
        binding.notificationPopup!!.clickableFrame.setOnClickListener { onNotificationTapped() }

        timeFormat = android.text.format.DateFormat.getTimeFormat(this)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        ambientController = AmbientMode.attachAmbientSupport(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        viewModel.albumArt.observe(this, albumArtObserver)
        viewModel.currentButtonConfig.observe(this, buttonConfigObserver)
        viewModel.preferences.observe(this, preferencesChangeObserver)
        viewModel.volume.observe(this, phoneVolumeListener)
        viewModel.popupVolumeBar.observe(this, volumeBarPopupListener)
        viewModel.closeActionsMenu.observe(this, closeDrawerListener)
        viewModel.openActionsMenu.observe(this, openActionsMenuListener)
        viewModel.closeApp.observe(this, closeAppListener)
        viewModel.notification.observe(this, notificationObserver)
        viewModel.customList.observe(this, customListListener)


        stemButtonsManager = StemButtonsManager(this, stemButtonListener)

        WatchInfoSender(this, true).sendWatchInfoToPhone()
        actionsMenuFragment = fragmentManager.findFragmentById(R.id.drawer_content) as ActionsMenuFragment
    }

    override fun onStart() {
        super.onStart()

        if (Preferences.getBoolean(preferences, MiscPreferences.ALWAYS_SHOW_TIME)) {
            handler.sendEmptyMessage(MESSAGE_UPDATE_CLOCK)
        }

        val crownDisableTime = Preferences.getInt(preferences, MiscPreferences.ROTATING_CROWN_OFF_PERIOD)
        if (crownDisableTime > 0) {
            rotatingInputDisabledUntil = System.currentTimeMillis() + crownDisableTime
        }

        viewModel.updateTimers()
    }

    override fun onStop() {
        if (isFinishing) {
            viewModel.sendManualCloseMessage()
        }

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

    private fun updateClock() {
        binding.ambientClock.text = timeFormat.format(java.util.Date())
    }

    private val musicStateObserver = Observer<Resource<MusicState>> {
        Timber.d("GUI Music State %s %s", it?.status, it?.data)
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

            val errorData = it.errorData
            if (errorData is ConnectionResult) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, errorData.errorCode, 1)?.show()
            }
        } else {
            binding.textArtist.text = ""
            binding.textTitle.text = getString(R.string.playback_stopped)
        }

        binding.textArtist.visibility = if (binding.textArtist.text.isEmpty()) View.GONE else View.VISIBLE
    }

    private val albumArtObserver = Observer<Bitmap?> {
        binding.albumArt.setImageBitmap(it)
    }

    private val buttonConfigObserver = Observer<WatchActionConfigProvider> { config ->
        if (config == null) {
            return@Observer
        }

        val topSingle = config.getAction(ButtonInfo(false, ScreenQuadrant.TOP, GESTURE_SINGLE_TAP))
        val bottomSingle = config.getAction(ButtonInfo(false, ScreenQuadrant.BOTTOM, GESTURE_SINGLE_TAP))
        val leftSingle = config.getAction(ButtonInfo(false, ScreenQuadrant.LEFT, GESTURE_SINGLE_TAP))
        val rightSingle = config.getAction(ButtonInfo(false, ScreenQuadrant.RIGHT, GESTURE_SINGLE_TAP))

        binding.iconTop.setImageDrawable(topSingle?.icon)
        binding.iconBottom.setImageDrawable(bottomSingle?.icon)
        binding.iconLeft.setImageDrawable(leftSingle?.icon)
        binding.iconRight.setImageDrawable(rightSingle?.icon)

        for (i in 0 until 4) {
            binding.fourWayTouch.enabledDoubleTaps[i] =
                    config.isActionActive(ButtonInfo(false, i, GESTURE_DOUBLE_TAP))
            binding.fourWayTouch.enabledLongTaps[i] =
                    config.isActionActive(ButtonInfo(false, i, GESTURE_LONG_TAP))
        }

        with(stemButtonsManager) {
            for (i in 0 until enabledDoublePressActions.size) {
                enabledDoublePressActions[i] = config.isActionActive(ButtonInfo(true, i, GESTURE_DOUBLE_TAP))
                enabledLongPressActions[i] = config.isActionActive(ButtonInfo(true, i, GESTURE_LONG_TAP))
            }

        }
    }

    private val preferencesChangeObserver = Observer<SharedPreferences> {
        if (it == null) {
            return@Observer
        }

        preferences = it

        stemButtonsManager.enableDoublePressInAmbient = !Preferences.getBoolean(preferences,
                MiscPreferences.DISABLE_PHYSICAL_DOUBLE_CLICK_IN_AMBIENT)

        if (!ambientController.isAmbient) {
            val alwaysDisplayClock = Preferences.getBoolean(preferences, MiscPreferences.ALWAYS_SHOW_TIME)

            if (alwaysDisplayClock) {
                binding.ambientClock.visibility = View.VISIBLE
                binding.iconTop.visibility = View.GONE
                handler.sendEmptyMessage(MESSAGE_UPDATE_CLOCK)
            } else {
                binding.iconTop.visibility = View.VISIBLE
                binding.ambientClock.visibility = View.GONE
            }
        }
    }

    private val notificationObserver = Observer<Notification> {
        if (it == null) {
            return@Observer
        }

        val notificationPopup = binding.notificationPopup!!

        notificationPopup.title.text = it.title
        notificationPopup.body.text = it.description
        notificationPopup.backgroundImage.setImageBitmap(it.background)

        showNotification()
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

    private val openActionsMenuListener = Observer<Unit> {
        actionsMenuFragment.refreshMenu(ActionsMenuFragment.MenuType.Actions)
        openMenuDrawer()
    }

    private val closeAppListener = Observer<Unit> {
        finish()
    }

    private val customListListener = Observer<CustomListWithBitmaps> {
        val lastListDisplayed = Preferences.getString(
                preferences,
                MiscPreferences.LAST_MENU_DISPLAYED
        ).toLong()

        if (!binding.actionDrawer.isClosed || lastListDisplayed != it.listTimestamp) {
            actionsMenuFragment.refreshMenu(ActionsMenuFragment.MenuType.Custom(it))
            openMenuDrawer()

            val editor = preferences.edit()
            Preferences.putString(
                    editor,
                    MiscPreferences.LAST_MENU_DISPLAYED,
                    it.listTimestamp.toString()
            )
            editor.apply()
        }
    }


    private val stemButtonListener = { buttonIndex: Int, gesture: Int ->
        if (gesture == GESTURE_DOUBLE_TAP) {
            handler.postDelayed(this::buzz, ViewConfiguration.getDoubleTapTimeout().toLong())
        } else {
            buzz()
        }

        viewModel.executeAction(ButtonInfo(true, buttonIndex, gesture))
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

        override fun onDrawerStateChanged(layout: WearableDrawerLayout, newState: Int) {
            if (newState == WearableDrawerView.STATE_DRAGGING && binding.actionDrawer.isClosed) {
                openDefaultListInDrawer()
            }
        }
    }

    private fun openDefaultListInDrawer() {
        val type = if (Preferences.getBoolean(
                        preferences,
                        MiscPreferences.OPEN_PLAYBACK_QUEUE_ON_SWIPE_UP
                )) {
            viewModel.openPlaybackQueue()

            ActionsMenuFragment.MenuType.Custom(
                    CustomListWithBitmaps(-1, "", emptyList())
            )
        } else {
            ActionsMenuFragment.MenuType.Actions
        }

        actionsMenuFragment.refreshMenu(type)
    }

    override fun getAmbientCallback(): AmbientMode.AmbientCallback = object : AmbientMode.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            stemButtonsManager.onEnterAmbient()
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

            binding.notificationPopup!!.backgroundImage.visibility = View.GONE
            binding.notificationPopup!!.solidBackground.background = ColorDrawable(Color.BLACK)

            binding.textArtist.displayTextOutline = true
            binding.textTitle.displayTextOutline = true

            binding.actionDrawer.controller.closeDrawer()
        }

        override fun onUpdateAmbient() {
            updateClock()
            viewModel.updateTimers()
        }

        override fun onExitAmbient() {
            stemButtonsManager.onExitAmbient()

            if (Preferences.getBoolean(preferences, MiscPreferences.ALWAYS_SHOW_TIME)) {
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

            binding.notificationPopup!!.backgroundImage.visibility = View.VISIBLE
            binding.notificationPopup!!.solidBackground.background = getDrawable(R.drawable.notification_popup_background)

            if (viewModel.musicState.value == null || (viewModel.musicState.value as Resource<MusicState>).status == Resource.Status.LOADING) {
                binding.loadingIndicator.visibility = View.VISIBLE
            }

            val crownDisableTime = Preferences.getInt(preferences, MiscPreferences.ROTATING_CROWN_OFF_PERIOD)
            if (crownDisableTime > 0) {
                rotatingInputDisabledUntil = System.currentTimeMillis() + crownDisableTime
            }

            binding.textArtist.displayTextOutline = false
            binding.textTitle.displayTextOutline = false
        }

    }

    override fun onGenericMotionEvent(ev: android.view.MotionEvent): Boolean {
        if (binding.actionDrawer.isOpened) {
            return false
        }

        if (rotatingInputDisabledUntil > System.currentTimeMillis()) {
            return false
        }

        if (ev.action == android.view.MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(
                ev
            )
        ) {
            val delta =
                -RotaryEncoder.getRotaryAxisValue(ev) * RotaryEncoder.getScaledScrollFactor(this)

            val multipler =
                Preferences.getInt(preferences, MiscPreferences.ROTATING_CROWN_SENSITIVITY) / 100f

            showVolumeBar()
            binding.volumeBar.incrementVolume(delta * 0.0025f * multipler)
            viewModel.updateVolume(binding.volumeBar.volume)


            return true
        }

        return super.onGenericMotionEvent(ev)
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return super.onKeyDown(keyCode, event)
        }

        if (binding.actionDrawer.isOpened) {
            return actionsMenuFragment.onKeyDown(keyCode, event)
        }

        if (stemButtonsManager.onKeyDown(keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (stemButtonsManager.onKeyUp(keyCode)) {
            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    private fun onNotificationTapped() {
        hideNotification()
    }

    private fun hideNotification() {
        val card = binding.notificationPopup!!.notificationCard
        card.animate().scaleX(0f).scaleY(0f).setDuration(200).withEndAction {
            card.visibility = View.GONE
        }.start()
        handler.removeMessages(MESSAGE_DISMISS_NOTIFICATION)
    }

    private fun showNotification() {
        val card = binding.notificationPopup!!.notificationCard
        card.animate().scaleX(1f).scaleY(1f).setDuration(200).withStartAction {
            card.visibility = View.VISIBLE
        }.start()

        val timeout = Preferences.getInt(preferences, MiscPreferences.NOTIFICATION_TIMEOUT)

        handler.removeMessages(MESSAGE_DISMISS_NOTIFICATION)
        if (timeout > 0) {
            handler.sendEmptyMessageDelayed(MESSAGE_DISMISS_NOTIFICATION, (timeout * 1000).toLong())
        }
    }

    private fun showVolumeBar() {
        binding.volumeBar.visibility = android.view.View.VISIBLE

        handler.removeMessages(MESSAGE_HIDE_VOLUME)
        handler.sendEmptyMessageDelayed(MESSAGE_HIDE_VOLUME, VOLUME_BAR_TIMEOUT)
    }

    fun buzz() {
        if (!Preferences.getBoolean(preferences, MiscPreferences.HAPTIC_FEEDBACK)) {
            return
        }

        VibratorCompat.vibrate(vibrator, 50)
    }

    override fun onUpwardsSwipe() {
        timber.log.Timber.d("UpwardsSwipe")

        binding.actionDrawer.controller.openDrawer()
        openDefaultListInDrawer()
    }

    override fun onSingleTap(quadrant: Int) {
        buzz()

        viewModel.executeAction(ButtonInfo(false, quadrant, GESTURE_SINGLE_TAP))
    }

    override fun onDoubleTap(quadrant: Int) {
        // Single tap vibration has delay, because it needs to wait to see if user presses
        // for the second time.
        // Introduce similar delay to double tap vibration to make it more apparent to the user
        // that he double pressed
        handler.postDelayed(this::buzz, ViewConfiguration.getDoubleTapTimeout().toLong())
        viewModel.executeAction(ButtonInfo(false, quadrant, GESTURE_DOUBLE_TAP))
    }

    override fun onLongTap(quadrant: Int) {
        buzz()
        viewModel.executeAction(ButtonInfo(false, quadrant, GESTURE_LONG_TAP))
    }

    private class TimeoutsHandler(val activity: WeakReference<MainActivity>) : Handler() {
        override fun handleMessage(msg: Message) {
            when {
                msg.what == MESSAGE_HIDE_VOLUME -> {
                    activity.get()?.binding?.volumeBar?.visibility = android.view.View.GONE
                }
                msg.what == MESSAGE_UPDATE_CLOCK -> {
                    removeMessages(MESSAGE_UPDATE_CLOCK)

                    val activity = activity.get() ?: return

                    activity.updateClock()

                    if (!activity.ambientController.isAmbient &&
                            Preferences.getBoolean(activity.preferences, MiscPreferences.ALWAYS_SHOW_TIME)) {
                        sendEmptyMessageDelayed(MESSAGE_UPDATE_CLOCK, 60_000)
                    }
                }
                msg.what == MESSAGE_DISMISS_NOTIFICATION -> {
                    activity.get()?.hideNotification()
                }
            }

        }
    }

    override fun getPhoneAppPresenceCapability(): String = CommPaths.PHONE_APP_CAPABILITY
}
