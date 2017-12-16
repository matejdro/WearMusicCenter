package com.matejdro.wearmusiccenter.config.buttons

import android.content.Context
import android.util.ArrayMap
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.config.WatchInfoProvider
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

@AutoFactory
class GlobalButtonConfig
constructor(playbackConfig: Boolean,
            @Provided context: Context,
            @Provided watchInfoProvider: WatchInfoProvider,
            @Provided customIconStorage: CustomIconStorage) : ButtonConfig {
    private val configMap = ArrayMap<ButtonInfo, PhoneAction>()
    private var commiting: Boolean = false
    private var commitAgain: Boolean = false

    private val diskButtonStorage: DiskButtonConfigStorage
    private val buttonTransmitter: ButtonConfigTransmitter

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

    override fun getAllActions(): Collection<Map.Entry<ButtonInfo, PhoneAction>> {
        return configMap.entries
    }

    init {
        val diskSuffix: String
        val watchSenderPath: String

        if (playbackConfig) {
            diskSuffix = "_playing"
            watchSenderPath = CommPaths.DATA_PLAYING_ACTION_CONFIG
        } else {
            diskSuffix = "_stopped"
            watchSenderPath = CommPaths.DATA_STOPPING_ACTION_CONFIG
        }

        diskButtonStorage = DiskButtonConfigStorage(context, diskSuffix)
        diskButtonStorage.loadButtons(this)

        buttonTransmitter = ButtonConfigTransmitter(this,
                context,
                watchInfoProvider,
                customIconStorage,
                watchSenderPath)
    }


    override fun commit() {
        if (commiting) {
            commitAgain = true
            return
        }

        commiting = true

        launch(UI) {
            launch(CommonPool) worker@ {
                diskButtonStorage.saveButtons(configMap.entries)
                buttonTransmitter.sendConfigToWatch(configMap.entries)
            }.join()

            commiting = false
            if (commitAgain) {
                commit()
            }
        }
    }
}