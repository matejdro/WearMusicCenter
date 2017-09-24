package com.matejdro.wearmusiccenter.view.buttonconfig

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.ScreenQuadrant
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.matejdro.wearmusiccenter.common.view.FourWayTouchLayout
import com.matejdro.wearmusiccenter.config.WatchInfoWithIcons
import com.matejdro.wearmusiccenter.config.buttons.ActionConfigStorage
import com.matejdro.wearmusiccenter.databinding.FragmentButtonConfigBinding
import com.matejdro.wearmusiccenter.databinding.ItemWatchButtonBinding
import com.matejdro.wearmusiccenter.view.TitledActivity
import com.matejdro.wearmusiccenter.view.mainactivity.ConfigActivityComponentProvider

class ButtonConfigFragment : Fragment(), FourWayTouchLayout.UserActionListener {
    companion object {
        private const val ARGUMENT_SETS_PLAYBACK_ACTIONS = "SetsPlaybackActions"

        fun newInstance(setsPlaybackActions: Boolean): ButtonConfigFragment {
            val arguments = Bundle()
            arguments.putBoolean(ARGUMENT_SETS_PLAYBACK_ACTIONS, setsPlaybackActions)

            val fragment = ButtonConfigFragment()
            fragment.arguments = arguments
            return fragment
        }
    }

    private var setsPlaybackActions: Boolean = false
    private var watchInfo: WatchInfoWithIcons? = null

    private lateinit var binding: FragmentButtonConfigBinding
    private lateinit var viewModel: ButtonConfigViewModel



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        val activity = activity
        if (activity is TitledActivity) {
            activity.updateActivityTitle(getString(if (setsPlaybackActions) R.string.playing_controls else R.string.stopped_controls))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setsPlaybackActions = arguments.getBoolean(ARGUMENT_SETS_PLAYBACK_ACTIONS)

        val configActivityComponent = (activity as ConfigActivityComponentProvider).provideConfigActivityComponent()
        val viewModelFactory = ButtonConfigViewModelFactory(setsPlaybackActions, configActivityComponent)

        viewModel = ViewModelProviders.of(this, viewModelFactory)[ButtonConfigViewModel::class.java]

        viewModel.watchInfoProvider.observe(this, watchInfoObserver)
        viewModel.buttonConfig.observe(this, buttonsConfigObserver)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_button_config, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        binding.fourWayTouch.listener = this
    }

    private val watchInfoObserver = Observer<WatchInfoWithIcons> {
        this.watchInfo = it

        while (binding.watchButtonContainer.childCount > 0) {
            binding.watchButtonContainer.removeViewAt(0)
        }

        val buttonsCount = it?.watchInfo?.buttonsCount ?: 0

        binding.captionPhysicalButtons.visibility = if (buttonsCount > 0) View.VISIBLE else View.GONE

        if (it == null) {
            return@Observer
        }

        if (it.watchInfo.roundWatch) {
            binding.watchDisplayBackground.setImageResource(R.drawable.watch_round_background)
            binding.watchDisplayBorder.setImageResource(R.drawable.watch_round_border)
        } else {
            binding.watchDisplayBackground.setImageResource(R.drawable.watch_square_background)
            binding.watchDisplayBorder.setImageResource(R.drawable.watch_square_border)
        }

        val inflater = LayoutInflater.from(activity)
        for (buttonIndex in 0 until buttonsCount) {
            val icon = watchInfo!!.icons[buttonIndex]
            icon?.setTint(Color.BLACK)

            val buttonInfo = watchInfo!!.watchInfo.buttonsList[buttonIndex]
            val buttonTitle = buttonInfo.label

            val buttonBinding = DataBindingUtil.inflate<ItemWatchButtonBinding>(inflater,
                    R.layout.item_watch_button,
                    binding.watchButtonContainer,
                    true)

            buttonBinding.button.text = buttonTitle
            buttonBinding.button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            buttonBinding.button.setOnClickListener {
                val buttonName = "$buttonTitle button"
                configureButton(true, buttonIndex, buttonName)
            }
        }
    }

    private val buttonsConfigObserver = Observer<ActionConfigStorage> {
        if (it == null) {
            return@Observer
        }

        val topAction = it.getScreenAction(ButtonInfo(false, ScreenQuadrant.TOP, GESTURE_SINGLE_TAP))
        binding.iconTop.setImageDrawable(topAction?.getIcon())

        val bottomAction = it.getScreenAction(ButtonInfo(false, ScreenQuadrant.BOTTOM, GESTURE_SINGLE_TAP))
        binding.iconBottom.setImageDrawable(bottomAction?.getIcon())

        val rightAction = it.getScreenAction(ButtonInfo(false, ScreenQuadrant.RIGHT, GESTURE_SINGLE_TAP))
        binding.iconRight.setImageDrawable(rightAction?.getIcon())

        val leftAction = it.getScreenAction(ButtonInfo(false, ScreenQuadrant.LEFT, GESTURE_SINGLE_TAP))
        binding.iconLeft.setImageDrawable(leftAction?.getIcon())
    }

    override fun onSingleTap(quadrant: Int) {
        val quadrantName = ScreenQuadrant.QUADRANT_NAMES[quadrant]
        val buttonName = "$quadrantName touch"

        configureButton(false, quadrant, buttonName)
    }

    fun configureButton(physicalButton : Boolean, buttonIndex : Int, buttonName : String) {
        val buttonInfo = ButtonInfo(physicalButton, buttonIndex, GESTURE_SINGLE_TAP)

        val gesturePicker = GesturePickerFragment.newInstance(setsPlaybackActions,
                buttonInfo,
                buttonName)
        gesturePicker.show(fragmentManager, "GesturePickerFragment")
    }

    private fun onButtonConfigurationFinished() {
        viewModel.commitConfig()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GesturePickerFragment.REQUEST_CODE_SAVE_NOTIFICATION &&
                resultCode == Activity.RESULT_OK) {
            onButtonConfigurationFinished()
        }
    }

    override fun onUpwardsSwipe() = Unit

    override fun onDoubleTap(quadrant: Int) = Unit
}