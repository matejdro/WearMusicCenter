package com.matejdro.wearmusiccenter.config

import android.content.Context
import android.util.ArrayMap
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber

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
        Timber.d(configMap.toString())

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

    private fun getIconSizePx() : Int {
        val density = watchInfoProvider.value?.watchInfo?.displayDensity ?: 0f

        return (density * ButtonConfigConstants.ICON_SIZE_DP).toInt()
    }

    /*private fun loadFromFile() {
        if (!storageFile.exists()) {
            return
        }

        val fileStream = FileInputStream(storageFile)
        fileStream.use {
            deserialize(this@DefaultActionConfigStorage.context, it)
        }
    }*/

    /*private fun toPutDataRequest(path: String): PutDataRequest {
        val putDataMap = PutDataMapRequest.create(path)

        writeScreenButtons(ButtonConfigConstants.SCREEN_BUTTON, screenButtons, putDataMap.dataMap::putByteArray)
        writeWatchButtons(ButtonConfigConstants.WATCH_BUTTON, watchButtons, putDataMap.dataMap::putByteArray)

        val putDataRequest = putDataMap.asPutDataRequest()

        writeScreenButtonsIcons(ButtonConfigConstants.SCREEN_BUTTON, screenButtons, putDataRequest.assets::put)

        return putDataRequest
    }


    private fun writeScreenButtons(prefix: String, buttons: Array<ButtonConfig>, write: (String, ByteArray?) -> Unit) {
        for (i in 0..3) {
            val button = buttons[i]

            val singleTapAction = button.singleTapAction
            if (singleTapAction != null) {
                write("$prefix.${ScreenQuadrant.QUADRANT_NAMES[i]}.${ButtonConfigConstants.SINGLE_TAP}",
                        ParcelPacker.getData(singleTapAction.serialize()))
            }

            val doubleTapAction = button.doubleTapAction
            if (doubleTapAction != null) {
                write("$prefix.${ScreenQuadrant.QUADRANT_NAMES[i]}.${ButtonConfigConstants.DOUBLE_TAP}",
                        ParcelPacker.getData(doubleTapAction.serialize()))
            }
        }
    }

    private fun writeScreenButtonsBundle(prefix: String, buttons: Array<ButtonConfig>,
                                         write: (String, PersistableBundle) -> Unit) {
        for (i in 0..3) {
            val button = buttons[i]

            val singleTapAction = button.singleTapAction
            if (singleTapAction != null) {
                write("$prefix.${ScreenQuadrant.QUADRANT_NAMES[i]}.${ButtonConfigConstants.SINGLE_TAP}",
                        singleTapAction.serialize())
            }

            val doubleTapAction = button.doubleTapAction
            if (doubleTapAction != null) {
                write("$prefix.${ScreenQuadrant.QUADRANT_NAMES[i]}.${ButtonConfigConstants.DOUBLE_TAP}",
                        doubleTapAction.serialize())
            }
        }
    }


    private fun writeWatchButtons(prefix: String, buttons: List<ButtonConfig>, write: (String, ByteArray?) -> Unit) {
        for (i in 0..watchButtons.size) {
            val button = buttons[i]

            val singleTapAction = button.singleTapAction
            if (singleTapAction != null) {
                write("$prefix.$i.${ButtonConfigConstants.SINGLE_TAP}",
                        ParcelPacker.getData(singleTapAction.serialize()))
            }

            val doubleTapAction = button.doubleTapAction
            if (doubleTapAction != null) {
                write("$prefix.$i.${ButtonConfigConstants.DOUBLE_TAP}",
                        ParcelPacker.getData(doubleTapAction.serialize()))
            }
        }
    }

    private fun writeWatchButtonsBundle(prefix: String, buttons: List<ButtonConfig>,
                                        write: (String, PersistableBundle) -> Unit) {
        for (i in 0..watchButtons.size) {
            val button = buttons[i]

            val singleTapAction = button.singleTapAction
            if (singleTapAction != null) {
                write("$prefix.$i.${ButtonConfigConstants.SINGLE_TAP}",
                        singleTapAction.serialize())
            }

            val doubleTapAction = button.doubleTapAction
            if (doubleTapAction != null) {
                write("$prefix.$i.${ButtonConfigConstants.DOUBLE_TAP}",
                        doubleTapAction.serialize())
            }
        }
    }

    private fun writeScreenButtonsIcons(prefix: String, buttons: Array<ButtonConfig>,
                                        write: (String, Asset?) -> Asset?) {

        val iconSizePx = getIconSizePx()
        if (iconSizePx < 0) {
            return
        }

        for (i in 0..3) {
            val button = buttons[i]

            val singleTapAction = button.singleTapAction
            if (singleTapAction != null) {
                val iconData = BitmapUtils.serialize(BitmapUtils.getBitmap(singleTapAction.getIcon(), iconSizePx, iconSizePx))
                write("$prefix.${ScreenQuadrant.QUADRANT_NAMES[i]}.${ButtonConfigConstants.SINGLE_TAP}",
                        Asset.createFromBytes(iconData))
            }

            val doubleTapAction = button.doubleTapAction
            if (doubleTapAction != null) {
                val iconData = BitmapUtils.serialize(BitmapUtils.getBitmap(doubleTapAction.getIcon(), iconSizePx, iconSizePx))
                write("$prefix.${ScreenQuadrant.QUADRANT_NAMES[i]}.${ButtonConfigConstants.DOUBLE_TAP}",
                        Asset.createFromBytes(iconData))
            }
        }
    }*/
}