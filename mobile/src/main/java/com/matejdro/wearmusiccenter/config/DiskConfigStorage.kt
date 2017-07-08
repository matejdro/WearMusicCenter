package com.matejdro.wearmusiccenter.config

import android.content.Context
import android.os.PersistableBundle
import android.support.annotation.WorkerThread
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.util.readInt
import com.matejdro.wearmusiccenter.util.writeInt
import com.matejdro.wearutils.messages.ParcelPacker
import timber.log.Timber
import java.io.*

class DiskConfigStorage(private val context: Context, fileSuffix : String) {
    private val storageFile = File(context.filesDir, "action_config" + fileSuffix)

    fun loadButtons(target : ActionConfigStorage) : Boolean {
        if (!storageFile.exists()) {
            return false
        }

        FileInputStream(storageFile).use {
            try {
                val configSize = it.readInt()

                if (configSize > 10_000_000) {
                    // Treat extraordinary long config as reading error
                    Timber.e("Config too large! Non-config stream?")
                    return false
                }

                val configData = ByteArray(configSize)
                it.read(configData)

                val configBundle = ParcelPacker.getParcelable(configData, PersistableBundle.CREATOR)
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
    }

    @WorkerThread
    fun saveButtons(buttons : Collection<Map.Entry<ButtonInfo, PhoneAction>>) : Boolean {
        try {
            FileOutputStream(storageFile).use {
                val configBundle = getConfigBundle(buttons)
                val configBundleBytes = ParcelPacker.getData(configBundle)

                it.writeInt(configBundleBytes.size)
                it.write(configBundleBytes)
            }
        } catch(e: IOException) {
            Timber.e(e, "Config writing error")
            return false
        }

        return true
    }


    private fun getConfigBundle(buttons : Collection<Map.Entry<ButtonInfo, PhoneAction>>): PersistableBundle {
        val configBundle = PersistableBundle()

        configBundle.putInt(ButtonConfigConstants.NUM_BUTTONS, buttons.size)

        for ((counter, button) in buttons.withIndex()) {
            val buttonInfo = button.key
            val buttonValue = button.value

            val buttonInfoKey = "${ButtonConfigConstants.BUTTON_INFO}.$counter"
            configBundle.putPersistableBundle(buttonInfoKey, buttonInfo.serialize())

            val buttonActionKey = "${ButtonConfigConstants.BUTTTON_ACTION}.$counter"
            configBundle.putPersistableBundle(buttonActionKey, buttonValue.serialize())
        }

        return configBundle
    }

    private fun unpackConfigBundle(bundle : PersistableBundle, target : ActionConfigStorage) {
        val numButtons = bundle.getInt(ButtonConfigConstants.NUM_BUTTONS, 0)
        for (buttonIndex in 0 until numButtons) {
            val buttonInfoKey = "${ButtonConfigConstants.BUTTON_INFO}.$buttonIndex"
            val buttonInfo = ButtonInfo(bundle.getPersistableBundle(buttonInfoKey))

            val buttonActionKey = "${ButtonConfigConstants.BUTTTON_ACTION}.$buttonIndex"
            val buttonAction = PhoneAction.deserialize<PhoneAction>(context, bundle.getPersistableBundle(buttonActionKey))

            target.saveButtonAction(buttonInfo, buttonAction)
        }
    }

}