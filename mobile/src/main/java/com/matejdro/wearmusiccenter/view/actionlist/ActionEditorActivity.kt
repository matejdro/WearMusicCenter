package com.matejdro.wearmusiccenter.view.actionlist

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.databinding.PopupActionEditorBinding
import com.matejdro.wearmusiccenter.view.actionconfigs.ActionConfigFragment
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerActivity
import com.matejdro.wearutils.miscutils.BitmapUtils
import dagger.android.AndroidInjection
import javax.inject.Inject

class ActionEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACTION = "Action"
        const val EXTRA_DELETING = "Deleting"

        const val STATE_ACTION = "Action"

        private const val REQUEST_CODE_PICK_ACTION = 8764
        private const val REQUEST_CODE_PICK_ICON = 5991
    }

    @Inject
    lateinit var iconStorage: CustomIconStorage

    private lateinit var binding: PopupActionEditorBinding

    private var currentAction: PhoneAction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        binding = PopupActionEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentActionBundle: PersistableBundle? = if (savedInstanceState != null) {
            savedInstanceState.getParcelable(STATE_ACTION)
        } else {
            intent.getParcelableExtra(EXTRA_ACTION)
        }

        if (currentActionBundle != null) {
            currentAction = PhoneAction.deserialize(this, currentActionBundle)
        }


        if (currentAction == null) {
            swapAction()
        } else {
            populateFields()
        }

        binding.icon.setOnClickListener { swapIcon() }
        binding.swapButton.setOnClickListener { swapAction() }
        binding.deleteButton.setOnClickListener { delete() }
        binding.okButton.setOnClickListener { save() }
        binding.cancelButton.setOnClickListener { cancel() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val currentAction = currentAction
        if (currentAction != null) {
            outState.putParcelable(STATE_ACTION, currentAction.serialize())
        }

        super.onSaveInstanceState(outState)
    }

    private fun swapAction() {
        val startIntent = Intent(this, ActionPickerActivity::class.java)
        startIntent.putExtra(ActionPickerActivity.EXTRA_DISPLAY_NONE, false)
        startActivityForResult(startIntent, REQUEST_CODE_PICK_ACTION)
    }

    fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    fun save() {
        val currentAction = currentAction
        if (currentAction == null) {
            cancel()
            return
        }

        val customTitle = binding.nameBox.text.toString()
        if (customTitle.isNotBlank() && customTitle != currentAction.title) {
            currentAction.customTitle = customTitle
        }

        @Suppress("UNCHECKED_CAST")
        (supportFragmentManager.findFragmentById(R.id.action_config_fragment) as? ActionConfigFragment<PhoneAction>)
                ?.save(currentAction)


        val returnIntent = Intent()
        returnIntent.putExtra(EXTRA_ACTION, currentAction.serialize())
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun delete() {
        val returnIntent = Intent()
        returnIntent.putExtra(EXTRA_DELETING, true)

        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun swapIcon() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
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
            AlertDialog.Builder(this)
                    .setTitle(R.string.icon_selection_title)
                    .setMessage(R.string.icon_selection_no_icon_pack)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
    }


    private fun requestStoragePermission() {
        AlertDialog.Builder(this)
                .setTitle(R.string.icon_selection_title)
                .setMessage(R.string.icon_selection_no_storage_permission)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener {
                    ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            REQUEST_CODE_PICK_ACTION)
                }
                .show()

    }


    private fun populateFields() {
        val currentAction = currentAction ?: return

        val icon = iconStorage[currentAction]
        if (icon is VectorDrawable) {
            binding.icon.setColorFilter(Color.BLACK)
        } else {
            binding.icon.clearColorFilter()
        }
        binding.icon.setImageDrawable(icon)

        binding.nameBox.setText(currentAction.title)

        val configFragmentClass = currentAction.configFragment
        if (configFragmentClass != null) {
            @Suppress("UNCHECKED_CAST")
            val configFragment: ActionConfigFragment<PhoneAction> = configFragmentClass.newInstance() as ActionConfigFragment<PhoneAction>
            supportFragmentManager.beginTransaction()
                    .replace(R.id.action_config_fragment, configFragment)
                    .commit()

            configFragment.load(currentAction)

        } else {
            val existingFragment = supportFragmentManager.findFragmentById(R.id.action_config_fragment)
            existingFragment?.let {
                supportFragmentManager.beginTransaction()
                        .remove(it)
                        .commit()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PICK_ACTION) {
            if (data == null) {
                if (currentAction == null) {
                    cancel()
                }
                return
            }

            val currentActionBundle = data.getParcelableExtra<PersistableBundle>(
                    ActionPickerActivity.EXTRA_ACTION_BUNDLE)
            if (currentActionBundle != null) {
                currentAction = PhoneAction.deserialize(this, currentActionBundle)
            }

            if (currentAction == null) {
                cancel()
            } else {
                populateFields()
            }

            return
        } else if (requestCode == REQUEST_CODE_PICK_ICON && data != null) {
            val currentAction = currentAction ?: return

            val iconUri = data.data!!
            val bitmap = BitmapUtils.getBitmap(BitmapUtils.getDrawableFromUri(this, iconUri)) ?: return

            currentAction.customIconUri = iconUri
            iconStorage.setIcon(iconUri, bitmap)
            populateFields()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (permissions.isNotEmpty() &&
                permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            swapIcon()
        }
    }

}
