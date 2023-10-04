package com.matejdro.wearmusiccenter.view.buttonconfig

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.ScreenQuadrant
import com.matejdro.wearmusiccenter.common.actions.StandardIcons
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.matejdro.wearmusiccenter.common.view.FourWayTouchLayout
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.config.WatchInfoWithIcons
import com.matejdro.wearmusiccenter.config.buttons.ButtonConfig
import com.matejdro.wearmusiccenter.databinding.FragmentButtonConfigBinding
import com.matejdro.wearmusiccenter.databinding.ItemWatchButtonBinding
import com.matejdro.wearmusiccenter.di.InjectableViewModelFactory
import com.matejdro.wearmusiccenter.view.TitledActivity
import dagger.Provides
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import javax.inject.Named

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
    private val viewModel: ButtonConfigViewModel by viewModels { viewModelFactory }

    @Inject
    lateinit var viewModelFactory: InjectableViewModelFactory<ButtonConfigViewModel>

    @Inject
    lateinit var customIconStorage: CustomIconStorage


    override fun onCreate(savedInstanceState: Bundle?) {
        setsPlaybackActions = requireArguments().getBoolean(ARGUMENT_SETS_PLAYBACK_ACTIONS)
        AndroidSupportInjection.inject(this)

        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        val activity = activity
        if (activity is TitledActivity) {
            activity.updateActivityTitle(getString(if (setsPlaybackActions) R.string.playing_controls else R.string.stopped_controls))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.watchInfoProvider.observe(viewLifecycleOwner, watchInfoObserver)
        viewModel.buttonConfig.observe(viewLifecycleOwner, buttonsConfigObserver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentButtonConfigBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.fourWayTouch.listener = this
    }

    private val watchInfoObserver = Observer<WatchInfoWithIcons?> {
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
            val buttonInfo = watchInfo!!.watchInfo.buttonsList[buttonIndex]
            val buttonTitle = buttonInfo.label
            val buttonCode = if (buttonInfo.hasCode()) buttonInfo.code else buttonIndex

            val icon = if (StandardIcons.hasIcon(buttonCode)) {
                ContextCompat.getDrawable(requireContext(), StandardIcons.getIcon(buttonCode))
            } else {
                watchInfo!!.icons[buttonCode]
            }

            icon?.setTint(Color.BLACK)

            val buttonBinding = ItemWatchButtonBinding.inflate(inflater, binding.watchButtonContainer, true)

            buttonBinding.button.text = buttonTitle
            buttonBinding.button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            buttonBinding.button.setOnClickListener {
                val buttonName = "$buttonTitle button"
                configureButton(true, buttonCode, buttonName, buttonInfo.supportsLongPress)
            }
        }
    }

    private val buttonsConfigObserver = Observer<ButtonConfig> {
        if (it == null) {
            return@Observer
        }

        val topAction = it.getScreenAction(ButtonInfo(false, ScreenQuadrant.TOP, GESTURE_SINGLE_TAP))
        setIcon(binding.iconTop, topAction)

        val bottomAction = it.getScreenAction(ButtonInfo(false, ScreenQuadrant.BOTTOM, GESTURE_SINGLE_TAP))
        setIcon(binding.iconBottom, bottomAction)

        val rightAction = it.getScreenAction(ButtonInfo(false, ScreenQuadrant.RIGHT, GESTURE_SINGLE_TAP))
        setIcon(binding.iconRight, rightAction)

        val leftAction = it.getScreenAction(ButtonInfo(false, ScreenQuadrant.LEFT, GESTURE_SINGLE_TAP))
        setIcon(binding.iconLeft, leftAction)
    }

    private fun setIcon(imageView: ImageView, phoneAction: PhoneAction?) {
        if (phoneAction == null) {
            imageView.setImageDrawable(null)
            return
        }

        imageView.setImageDrawable(customIconStorage[phoneAction])
    }

    override fun onSingleTap(quadrant: Int) {
        val quadrantName = ScreenQuadrant.QUADRANT_NAMES[quadrant]
        val buttonName = "$quadrantName touch"

        configureButton(false, quadrant, buttonName, true)
    }

    private fun configureButton(physicalButton: Boolean, buttonCode: Int, buttonName: String, supportsLongPress: Boolean) {
        val buttonInfo = ButtonInfo(physicalButton, buttonCode, GESTURE_SINGLE_TAP)

        val gesturePicker = GesturePickerFragment.newInstance(setsPlaybackActions,
                buttonInfo,
                buttonName,
                supportsLongPress)
        gesturePicker.show(requireFragmentManager(), "GesturePickerFragment")
    }

    private fun onButtonConfigurationFinished() {
        viewModel.commitConfig()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GesturePickerFragment.REQUEST_CODE_SAVE_NOTIFICATION &&
                resultCode == Activity.RESULT_OK) {
            onButtonConfigurationFinished()
        }
    }

    override fun onUpwardsSwipe() = Unit

    override fun onDoubleTap(quadrant: Int) = Unit

    override fun onLongTap(quadrant: Int) = Unit

    @dagger.Module
    class Module {
        @Provides
        @Named(ButtonConfigViewModel.ARG_DISPLAY_PLAYBACK_ACTIONS)
        fun displayPlaybackActions(configFragment: ButtonConfigFragment) = configFragment.setsPlaybackActions
    }
}
