package com.matejdro.wearmusiccenter.config

import android.content.Context
import com.matejdro.wearmusiccenter.actions.appplay.AppPlayAction
import com.matejdro.wearmusiccenter.actions.appplay.AppPlayPickerAction
import com.matejdro.wearmusiccenter.actions.playback.PauseAction
import com.matejdro.wearmusiccenter.actions.playback.PlayAction
import com.matejdro.wearmusiccenter.actions.playback.SkipToNextAction
import com.matejdro.wearmusiccenter.actions.playback.SkipToPrevAction
import com.matejdro.wearmusiccenter.actions.volume.DecreaseVolumeAction
import com.matejdro.wearmusiccenter.actions.volume.IncreaseVolumeAction
import com.matejdro.wearmusiccenter.common.ScreenQuadrant
import com.matejdro.wearmusiccenter.common.buttonconfig.ButtonInfo
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_DOUBLE_TAP
import com.matejdro.wearmusiccenter.common.buttonconfig.GESTURE_SINGLE_TAP
import com.matejdro.wearmusiccenter.config.actionlist.ActionListStorage
import javax.inject.Inject

class DefaultConfigGenerator @Inject constructor(private val context : Context,
                                                 private val watchInfoProvider: WatchInfoProvider) {

    fun generateDefaultButtons(actionConfigProvider: ActionConfigProvider) {
        val playingConfig = actionConfigProvider.getPlayingConfig()
        val stoppedConfig = actionConfigProvider.getStoppedConfig()

        playingConfig.saveButtonAction(
                ButtonInfo(false, ScreenQuadrant.TOP, GESTURE_SINGLE_TAP),
                IncreaseVolumeAction(context))
        playingConfig.saveButtonAction(
                ButtonInfo(false, ScreenQuadrant.BOTTOM, GESTURE_SINGLE_TAP),
                DecreaseVolumeAction(context))

        playingConfig.saveButtonAction(
                ButtonInfo(false, ScreenQuadrant.LEFT, GESTURE_SINGLE_TAP),
                PauseAction(context))
        playingConfig.saveButtonAction(
                ButtonInfo(false, ScreenQuadrant.LEFT, GESTURE_DOUBLE_TAP),
                SkipToPrevAction(context))

        playingConfig.saveButtonAction(
                ButtonInfo(false, ScreenQuadrant.RIGHT, GESTURE_SINGLE_TAP),
                SkipToNextAction(context))

        stoppedConfig.saveButtonAction(
                ButtonInfo(false, ScreenQuadrant.LEFT, GESTURE_SINGLE_TAP),
                PlayAction(context))

        val musicApps = AppPlayPickerAction.getAllMusicApps(context)
        musicApps
                .take(3)
                .forEachIndexed { index, componentName ->
                    stoppedConfig.saveButtonAction(
                            ButtonInfo(false, ScreenQuadrant.TOP + index, GESTURE_SINGLE_TAP),
                            AppPlayAction(context, componentName))

                }

        val watchInfo = watchInfoProvider.value
        val numPhysicalButtons = watchInfo?.watchInfo?.buttonsCount ?: 0

        if (numPhysicalButtons > 1) {
            val lastButton = numPhysicalButtons - 1
            // Last button is usually the bottom one - assign play action to it
            playingConfig.saveButtonAction(
                    ButtonInfo(true, lastButton, GESTURE_SINGLE_TAP),
                    PauseAction(context))
            stoppedConfig.saveButtonAction(
                    ButtonInfo(true, lastButton, GESTURE_SINGLE_TAP),
                    PlayAction(context))

            // Assign "next track" action to the first button
            playingConfig.saveButtonAction(
                    ButtonInfo(true, 0, GESTURE_SINGLE_TAP),
                    SkipToNextAction(context))
        } else if (numPhysicalButtons > 0) {
            // We only have one button. Assign play/pause to it.
            playingConfig.saveButtonAction(
                    ButtonInfo(true, 0, GESTURE_SINGLE_TAP),
                    PauseAction(context))
            stoppedConfig.saveButtonAction(
                    ButtonInfo(true, 0, GESTURE_SINGLE_TAP),
                    PlayAction(context))
        }
    }

    fun generateDefaultActionList(actionListStorage: ActionListStorage) {
        actionListStorage.actions =
                AppPlayPickerAction.getAllMusicApps(context)
                        .map { AppPlayAction(context, it) }
    }
}