package com.matejdro.wearmusiccenter.view.buttonconfig

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import androidx.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.NullAction
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.buttonconfig.*
import com.matejdro.wearmusiccenter.config.ActionConfig
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.config.buttons.ButtonConfig
import com.matejdro.wearmusiccenter.databinding.PopupGesturePickerBinding
import com.matejdro.wearmusiccenter.di.LocalActivityConfig
import com.matejdro.wearmusiccenter.view.ActivityResultReceiver
import com.matejdro.wearutils.miscutils.BitmapUtils
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class GesturePickerFragment : DialogFragment() {
    companion object {
        const val REQUEST_CODE_SAVE_NOTIFICATION = 1578

        private const val PARAM_SETS_PLAYBACK_ACTIONS = "SetsPlaybackActions"
        private const val PARAM_BUTTON_INFO = "ButtonInfo"
        private const val PARAM_BUTTON_NAME = "ButtonName"

        private const val REQUEST_CODE_PICK_ACTION = 5891
        private const val REQUEST_CODE_PICK_ACTION_TO = REQUEST_CODE_PICK_ACTION + NUM_BUTTON_GESTURES

        private const val REQUEST_CODE_PICK_ICON = 5991

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

    private lateinit var actions: Array<PhoneAction?>

    @Inject
    @field:LocalActivityConfig
    lateinit var config: ActionConfig

    @Inject
    lateinit var customIconStorage: CustomIconStorage

    private lateinit var buttonConfig: ButtonConfig
    private lateinit var buttons: Array<Button>
    private var anythingChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseButtonInfo = ButtonInfo(arguments!!.getParcelable<PersistableBundle>(PARAM_BUTTON_INFO)!!)
        buttonName = arguments!!.getString(PARAM_BUTTON_NAME)!!
        setsPlaybackButtons = arguments!!.getBoolean(PARAM_SETS_PLAYBACK_ACTIONS)

        AndroidSupportInjection.inject(this)

        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Dialog_Short)

        buttonConfig = if (setsPlaybackButtons)
            config.getPlayingConfig()
        else
            config.getStoppedConfig()

        actions = Array(NUM_BUTTON_GESTURES) {
            buttonConfig.getScreenAction(baseButtonInfo.copy(gesture = it))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.popup_gesture_picker, container, false)
        binding.fragment = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog!!.setCanceledOnTouchOutside(true)
        dialog!!.setTitle(buttonName)

        buttons = arrayOf(binding.singlePressButton, binding.doublePressButton, binding.longPressButton)

        updateButton(binding.singlePressButton, GESTURE_SINGLE_TAP)
        updateButton(binding.doublePressButton, GESTURE_DOUBLE_TAP)
        updateButton(binding.longPressButton, GESTURE_LONG_TAP)
    }

    fun changeAction(gesture: Int) {
        val requestCode = REQUEST_CODE_PICK_ACTION + gesture
        startActivityForResult(Intent(activity, ActionPickerActivity::class.java), requestCode)

    }

    private fun updateButton(button: Button, @ButtonGesture gesture: Int) {
        val phoneAction = actions[gesture]


        updateButton(button, phoneAction)
    }

    private fun updateButton(button: Button, phoneAction: PhoneAction?) {
        var mutableAction = phoneAction

        if (mutableAction == null) {
            mutableAction = NullAction(activity!!)
        }

        var icon = customIconStorage[mutableAction]
        if (icon is VectorDrawable) {
            icon = icon.mutate()
            icon.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY)
        }

        val iconSize = resources.getDimensionPixelSize(R.dimen.action_icon_size)
        icon.setBounds(0, 0, iconSize, iconSize)

        button.text = mutableAction.title
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

    fun startIconSelection() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission()
            return
        }

        try {
            var intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"

            intent = Intent.createChooser(intent, getString(R.string.icon_selection_title))

            startActivityForResult(intent, REQUEST_CODE_PICK_ICON)
        } catch (ignored: ActivityNotFoundException) {
            AlertDialog.Builder(context!!)
                    .setTitle(R.string.icon_selection_title)
                    .setMessage(R.string.icon_selection_no_icon_pack)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
    }

    private fun requestStoragePermission() {
        AlertDialog.Builder(context!!)
                .setTitle(R.string.icon_selection_title)
                .setMessage(R.string.icon_selection_no_storage_permission)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            REQUEST_CODE_PICK_ACTION)
                }
                .show()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        if (requestCode in REQUEST_CODE_PICK_ACTION until REQUEST_CODE_PICK_ACTION_TO) {
            val gesture = requestCode - REQUEST_CODE_PICK_ACTION
            val actionBundle = data.getParcelableExtra<PersistableBundle>(ActionPickerActivity.EXTRA_ACTION_BUNDLE)
            val action = PhoneAction.deserialize<PhoneAction>(activity!!, actionBundle)

            anythingChanged = anythingChanged || action != actions[gesture]

            actions[gesture] = action
            updateButton(buttons[gesture],  action)
        } else if (requestCode == REQUEST_CODE_PICK_ICON) {
            val iconUri = data.data!!

            val action = actions[GESTURE_SINGLE_TAP].let {
                if (it == null) {
                    val newAction = NullAction(context!!)
                    actions[GESTURE_SINGLE_TAP] = newAction
                    newAction
                } else {
                    it
                }

            }

            val bitmap = BitmapUtils.getBitmap(BitmapUtils.getDrawableFromUri(activity, iconUri)) ?: return

            action.customIconUri = iconUri
            customIconStorage.setIcon(iconUri, bitmap)

            anythingChanged = true
            updateButton(buttons[GESTURE_SINGLE_TAP], action)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (permissions.isNotEmpty() &&
                permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startIconSelection()
        }
    }
}
