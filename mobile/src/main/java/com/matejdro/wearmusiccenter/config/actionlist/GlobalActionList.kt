package com.matejdro.wearmusiccenter.config.actionlist

import android.content.Context
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

class GlobalActionList @Inject constructor(context: Context,
                                           watchInfoProvider: WatchInfoProvider,
                                           iconStorage: CustomIconStorage) : ActionList {
    override var actions: List<PhoneAction> = emptyList()
    private val diskStorage = DiskActionListStorage(context)

    private var committing: Boolean = false
    private var commitAgain: Boolean = false


    private val transmitter: ActionListTransmitter

    init {
        diskStorage.loadActions(this)

        transmitter = ActionListTransmitter(this, iconStorage, context, watchInfoProvider)
    }

    override fun commit() {
        if (committing) {
            commitAgain = true
            return
        }

        committing = true

        launch(UI) {
            launch(CommonPool) {
                diskStorage.saveActions(actions)
                transmitter.sendConfigToWatch(actions)
            }.join()

            committing = false
            if (commitAgain) {
                commit()
            }
        }
    }
}