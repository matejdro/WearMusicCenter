package com.matejdro.wearmusiccenter.common.actions

import android.view.KeyEvent
import com.matejdro.common.R
import com.matejdro.wearmusiccenter.common.buttonconfig.SpecialButtonCodes

object StandardIcons {
    private val iconMap = mapOf(
            StandardActions.ACTION_PLAY to R.drawable.action_play,
            StandardActions.ACTION_PAUSE to R.drawable.action_pause,
            StandardActions.ACTION_SKIP_TO_PREV to R.drawable.action_skip_prev,
            StandardActions.ACTION_SKIP_TO_NEXT to R.drawable.action_skip_next,
            StandardActions.ACTION_VOLUME_UP to R.drawable.action_volume_up,
            StandardActions.ACTION_VOLUME_DOWN to R.drawable.action_volume_down,
            StandardActions.ACTION_OPEN_MENU to R.drawable.action_open_menu,
            StandardActions.ACTION_SKIP_30_SECONDS to R.drawable.action_skip_30_seconds,
            StandardActions.ACTION_REVERSE_30_SECONDS to R.drawable.action_reverse_30_seconds,

            getButtonKey(KeyEvent.KEYCODE_BACK) to R.drawable.button_back,
            getButtonKey(SpecialButtonCodes.TURN_ROTARY_CW) to R.drawable.button_turn_cw,
            getButtonKey(SpecialButtonCodes.TURN_ROTARY_CCW) to R.drawable.button_turn_ccw
    )

    fun hasIcon(key: String): Boolean = iconMap.containsKey(key)
    fun getIcon(key: String): Int = iconMap[key] ?: 0

    fun hasIcon(buttonId: Int): Boolean = iconMap.containsKey(getButtonKey(buttonId))
    fun getIcon(buttonId: Int): Int = iconMap[getButtonKey(buttonId)] ?: 0

    private fun getButtonKey(id: Int): String {
        return "$BUTTON_PREFIX$id"
    }
}

private const val BUTTON_PREFIX = "BUTTON_"