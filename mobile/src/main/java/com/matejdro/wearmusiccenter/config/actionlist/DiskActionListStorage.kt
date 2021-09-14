package com.matejdro.wearmusiccenter.config.actionlist

import android.content.Context
import android.os.PersistableBundle
import androidx.annotation.WorkerThread
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.config.buttons.ConfigConstants
import com.matejdro.wearmusiccenter.util.BundleFileSerialization
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

class DiskActionListStorage @Inject constructor(private val context: Context) {
    private val storageFile = File(context.filesDir, "actions_list")

    fun loadActions(target: ActionList): Boolean {

        try {
            val bundle = BundleFileSerialization.readFromFile(storageFile) ?: return false
            unpackActionListBundle(bundle, target)
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
    fun saveActions(actions: List<PhoneAction>): Boolean {
        try {
            BundleFileSerialization.writeToFile(getActionListBundle(actions), storageFile)
        } catch(e: IOException) {
            Timber.e(e, "Config writing error")
            return false
        }

        return true
    }

    private fun getActionListBundle(actions: List<PhoneAction>): PersistableBundle {
        val bundle = PersistableBundle()

        bundle.putInt(ConfigConstants.NUM_ACTIONS, actions.size)

        for ((counter, action) in actions.withIndex()) {

            val key = "$counter"
            bundle.putPersistableBundle(key, action.serialize())
        }

        return bundle
    }

    private fun unpackActionListBundle(bundle: PersistableBundle, target: ActionList) {
        val numActions = bundle.getInt(ConfigConstants.NUM_ACTIONS, 0)
        val actions = (0 until numActions)
                .map { it.toString() }
                .mapNotNull {
                    PhoneAction.deserialize(context, bundle.getPersistableBundle(it))
                }
                .toList()

        target.actions = actions
    }

}
