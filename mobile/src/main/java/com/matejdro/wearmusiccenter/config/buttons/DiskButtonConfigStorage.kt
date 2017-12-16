package com.matejdro.wearmusiccenter.config.buttons

import android.content.Context
import android.os.PersistableBundle
import android.support.annotation.WorkerThread
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.util.BundleFileSerialization
import timber.log.Timber
import java.io.File
import java.io.IOException

@AutoFactory
class DiskButtonConfigStorage(@Provided private val context: Context, fileSuffix: String) {
    private val storageFile = File(context.filesDir, "action_config" + fileSuffix)

    fun loadButtons(target: ButtonConfig): Boolean {
        try {
            val configBundle = BundleFileSerialization.readFromFile(storageFile) ?: return false
            unpackConfigBundle(configBundle, target)

            return true
        } catch(e: IOException) {
            Timber.e(e, "Config reading error")
            return false
        } catch(e: RuntimeException) {
            // RuntimeException is thrown if marshalling fails
            Timber.e(e, "Config reading error")
            return false
        }
    }

    @WorkerThread
    fun saveButtons(buttons : Collection<Map.Entry<ButtonInfo, PhoneAction>>) : Boolean {
        try {
            BundleFileSerialization.writeToFile(getConfigBundle(buttons), storageFile)
        } catch(e: IOException) {
            Timber.e(e, "Config writing error")
            return false
        }

        return true
    }


    private fun getConfigBundle(buttons : Collection<Map.Entry<ButtonInfo, PhoneAction>>): PersistableBundle {
        val configBundle = PersistableBundle()

        configBundle.putInt(ConfigConstants.NUM_BUTTONS, buttons.size)

        for ((counter, button) in buttons.withIndex()) {
            val buttonInfo = button.key
            val buttonValue = button.value

            val buttonInfoKey = "${ConfigConstants.BUTTON_INFO}.$counter"
            configBundle.putPersistableBundle(buttonInfoKey, buttonInfo.serialize())

            val buttonActionKey = "${ConfigConstants.BUTTON_ACTION}.$counter"
            configBundle.putPersistableBundle(buttonActionKey, buttonValue.serialize())
        }

        return configBundle
    }

    private fun unpackConfigBundle(bundle: PersistableBundle, target: ButtonConfig) {
        val numButtons = bundle.getInt(ConfigConstants.NUM_BUTTONS, 0)
        for (buttonIndex in 0 until numButtons) {
            val buttonInfoKey = "${ConfigConstants.BUTTON_INFO}.$buttonIndex"
            val buttonInfo = ButtonInfo(bundle.getPersistableBundle(buttonInfoKey))

            val buttonActionKey = "${ConfigConstants.BUTTON_ACTION}.$buttonIndex"
            val buttonAction = PhoneAction.deserialize<PhoneAction>(context, bundle.getPersistableBundle(buttonActionKey))

            target.saveButtonAction(buttonInfo, buttonAction)
        }
    }

}