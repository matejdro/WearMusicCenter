package com.matejdro.wearmusiccenter.view.buttonconfig

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.NullAction
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.buttonconfig.*
import com.matejdro.wearmusiccenter.config.ActionConfigProvider
import com.matejdro.wearmusiccenter.config.ActionConfigStorage
import com.matejdro.wearmusiccenter.databinding.PopupGesturePickerBinding
import com.matejdro.wearmusiccenter.view.ActivityResultReceiver
import com.matejdro.wearmusiccenter.view.mainactivity.ConfigActivityComponentProvider
import timber.log.Timber
import javax.inject.Inject

class GesturePickerFragment : DialogFragment() {
    companion object {
        const val REQUEST_CODE_SAVE_NOTIFICATION = 1578

        private const val PARAM_SETS_PLAYBACK_ACTIONS = "SetsPlaybackActions"
        private const val PARAM_BUTTON_INFO = "ButtonInfo"
        private const val PARAM_BUTTON_NAME = "ButtonName"

        private const val REQUEST_CODE_PICK_ACTION = 5891
        private const val REQUEST_CODE_PICK_ACTION_TO = REQUEST_CODE_PICK_ACTION + NUM_BUTTON_GESTURES

        fun newInstance(setsPlaybackButtons: Boolean, baseButtonInfo: ButtonInfo, buttonName: String): GesturePickerFragment {
            val fragment = GesturePickerFragment()

            val args = Bundle()
            args.putBoolean(PARAM_SETS_PLAYBACK_ACTIONS, setsPlaybackButtons)
            args.putParcelable(PARAM_BUTTON_INFO, baseButtonInfo.serialize())
            args.putString(PARAM_BUTTON_NAME, buttonName)

            fragment.arguments = args
            return fragment
        }
    }

    private var setsPlaybackButtons: Boolean = false
    private lateinit var binding: PopupGesturePickerBinding
    private lateinit var baseButtonInfo: ButtonInfo
    private lateinit var buttonName: String

    private lateinit var actions : Array<PhoneAction?>

    @Inject
    lateinit var configProvider: ActionConfigProvider

    private lateinit var buttonConfig: ActionConfigStorage
    private lateinit var buttons : Array<Button>
    private var anythingChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (activity as ConfigActivityComponentProvider)
                .provideConfigActivityComponent().inject(this)

        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Dialog_Short)

        baseButtonInfo = ButtonInfo(arguments.getParcelable<PersistableBundle>(PARAM_BUTTON_INFO))
        buttonName = arguments.getString(PARAM_BUTTON_NAME)
        setsPlaybackButtons = arguments.getBoolean(PARAM_SETS_PLAYBACK_ACTIONS)

        buttonConfig = if (setsPlaybackButtons)
            configProvider.getPlayingConfig()
        else
            configProvider.getStoppedConfig()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.popup_gesture_picker, container, false)
        binding.fragment = this

        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setCanceledOnTouchOutside(true)
        dialog.setTitle(buttonName)

        actions = Array(NUM_BUTTON_GESTURES) {
            buttonConfig.getScreenAction(baseButtonInfo.copy(gesture = it))
        }

        buttons = arrayOf(binding.singlePressButton, binding.doublePressButton)

        applyButton(binding.singlePressButton, GESTURE_SINGLE_TAP)
        applyButton(binding.doublePressButton, GESTURE_DOUBLE_TAP)
    }

    fun changeAction(gesture : Int) {
        val requestCode = REQUEST_CODE_PICK_ACTION + gesture
        startActivityForResult(Intent(activity, ActionPickerActivity::class.java), requestCode)

    }

    private fun applyButton(button: Button, @ButtonGesture gesture: Int) {
        val phoneAction = actions[gesture]
        applyButton(button, phoneAction)
    }

    private fun applyButton(button: Button, phoneAction: PhoneAction?) {
        var mutableAction = phoneAction

        if (mutableAction == null) {
            mutableAction = NullAction(activity)
        }

        var icon = mutableAction.getIcon()
        if (icon is VectorDrawable) {
            icon = icon.mutate()
            icon.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY)
        }

        // Wrap icon into another dummy drawable, so setting its bounds will not affect the icon
        icon = InsetDrawable(icon, 0)

        val iconSize = resources.getDimensionPixelSize(R.dimen.action_icon_size)
        icon.setBounds(0, 0, iconSize, iconSize)

        button.text = mutableAction.getName()
        button.setCompoundDrawables(icon, null, null, null)
    }

    fun save() {
        if (anythingChanged) {
            for ((gesture, action) in actions.withIndex()) {
                val buttonInfo = baseButtonInfo.copy(gesture = gesture)

                buttonConfig.saveButtonAction(buttonInfo, action)
            }

            (activity as ActivityResultReceiver)
                    .onActivityResult(REQUEST_CODE_SAVE_NOTIFICATION, Activity.RESULT_OK, null)
        }

        dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null &&
                requestCode >= REQUEST_CODE_PICK_ACTION &&
                requestCode < REQUEST_CODE_PICK_ACTION_TO) {

            val gesture = requestCode - REQUEST_CODE_PICK_ACTION
            val actionBundle = data.getParcelableExtra<PersistableBundle>(ActionPickerActivity.EXTRA_ACTION_BUNDLE)
            val action = PhoneAction.deserialize<PhoneAction>(activity, actionBundle)

            anythingChanged = anythingChanged || action != actions[gesture]

            actions[gesture] = action
            applyButton(buttons[gesture], action)
        }
    }
}