package com.matejdro.wearmusiccenter.config

import android.content.Context
import android.util.ArrayMap
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class DefaultActionConfigStorage
constructor(playbackConfig: Boolean, context : Context, private val watchInfoProvider: WatchInfoProvider) : ActionConfigStorage {
    private val configMap = ArrayMap<ButtonInfo, PhoneAction>()
    private var commiting : Boolean = false
    private var commitAgain : Boolean = false

    private val diskStorage : DiskConfigStorage
    private val watchSender : WatchConfigSender

    override fun saveButtonAction(buttonInfo: ButtonInfo, action: PhoneAction?) {
        if (action == null) {
            configMap.remove(buttonInfo)
        } else {
            configMap[buttonInfo] = action
        }
    }

    override fun getScreenAction(buttonInfo: ButtonInfo): PhoneAction? {
        return configMap[buttonInfo]
    }

    init {
        val diskSuffix : String
        val watchSenderPath : String

        if (playbackConfig) {
            diskSuffix = "_playing"
            watchSenderPath = CommPaths.DATA_PLAYING_ACTION_CONFIG
        } else {
            diskSuffix = "_stopped"
            watchSenderPath = CommPaths.DATA_STOPPING_ACTION_CONFIG
        }

        watchSender = WatchConfigSender(context, watchInfoProvider, watchSenderPath)

        diskStorage = DiskConfigStorage(context, diskSuffix)
        diskStorage.loadButtons(this)
    }


    override fun commit() {
        if (commiting) {
            commitAgain = true
            return
        }

        commiting = true

        launch(UI) {
            launch(CommonPool) worker@ {
                diskStorage.saveButtons(configMap.entries)
                watchSender.sendConfigToWatch(configMap.entries)
            }.join()

            commiting = false
            if (commitAgain) {
                commit()
            }
        }
    }
}