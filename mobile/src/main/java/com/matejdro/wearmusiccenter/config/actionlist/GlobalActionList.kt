package com.matejdro.wearmusiccenter.config.actionlist

import android.content.Context
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.util.launchWithPlayServicesErrorHandling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GlobalActionList @Inject constructor(actionListTransmitterFactory: ActionListTransmitterFactory,
                                           private val diskStorage: DiskActionListStorage,
                                           private val context: Context) : ActionList {
    override var actions: List<PhoneAction> = emptyList()

    private var committing: Boolean = false
    private var commitAgain: Boolean = false


    private val transmitter: ActionListTransmitter

    init {
        diskStorage.loadActions(this)

        transmitter = actionListTransmitterFactory.create(this)
    }

    override fun commit() {
        if (committing) {
            commitAgain = true
            return
        }

        committing = true

        GlobalScope.launchWithPlayServicesErrorHandling(context, Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                diskStorage.saveActions(actions)
                transmitter.sendConfigToWatch(actions)
            }

            committing = false
            if (commitAgain) {
                commit()
            }
        }
    }
}
