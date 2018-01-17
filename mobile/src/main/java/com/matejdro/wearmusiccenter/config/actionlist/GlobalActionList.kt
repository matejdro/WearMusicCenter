package com.matejdro.wearmusiccenter.config.actionlist

import com.matejdro.wearmusiccenter.actions.PhoneAction
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

class GlobalActionList @Inject constructor(actionListTransmitterFactory: ActionListTransmitterFactory,
                                           private val diskStorage: DiskActionListStorage) : ActionList {
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

        launch(UI) {
            async {
                diskStorage.saveActions(actions)
                transmitter.sendConfigToWatch(actions)
            }.await()

            committing = false
            if (commitAgain) {
                commit()
            }
        }
    }
}