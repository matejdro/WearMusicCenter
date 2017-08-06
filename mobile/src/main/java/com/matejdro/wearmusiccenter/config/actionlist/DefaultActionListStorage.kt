package com.matejdro.wearmusiccenter.config.actionlist

import android.content.Context
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class DefaultActionListStorage(context: Context, watchInfoProvider: WatchInfoProvider) : ActionListStorage {
    override var actions: List<PhoneAction> = emptyList()
    private val diskStorage = DiskActionListStorage(context)

    private var committing: Boolean = false
    private var commitAgain: Boolean = false


    private val watchSender = WatchActionListSender(context, watchInfoProvider)

    init {
        diskStorage.loadActions(this)
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
                watchSender.sendConfigToWatch(actions)
            }.join()

            committing = false
            if (commitAgain) {
                commit()
            }
        }
    }
}