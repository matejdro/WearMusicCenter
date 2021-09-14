package com.matejdro.wearmusiccenter.config.buttons

import android.util.ArrayMap
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AutoFactory
class GlobalButtonConfig
constructor(playbackConfig: Boolean,
            @Provided diskButtonConfigStorageFactory: DiskButtonConfigStorageFactory,
            @Provided buttonConfigTransmitterFactory: ButtonConfigTransmitterFactory) : ButtonConfig {
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
        return configMap[buttonInfo] ?: configMap[buttonInfo.getLegacyButtonInfo()]
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

        diskButtonStorage = diskButtonConfigStorageFactory.create(diskSuffix)
        diskButtonStorage.loadButtons(this)

        buttonTransmitter = buttonConfigTransmitterFactory.create(this, watchSenderPath)
    }


    override fun commit() {
        if (commiting) {
            commitAgain = true
            return
        }

        commiting = true

        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) worker@ {
                diskButtonStorage.saveButtons(configMap.entries)
                buttonTransmitter.sendConfigToWatch(configMap.entries)
            }

            commiting = false
            if (commitAgain) {
                commit()
            }
        }
    }
}
