package com.matejdro.wearmusiccenter.common.actions

import com.matejdro.common.R

object StandardIcons {
    private val iconMap = mapOf(
            StandardActions.ACTION_PLAY to R.drawable.action_play,
            StandardActions.ACTION_PAUSE to R.drawable.action_pause,
            StandardActions.ACTION_SKIP_TO_PREV to R.drawable.action_skip_prev,
            StandardActions.ACTION_SKIP_TO_NEXT to R.drawable.action_skip_next,
            StandardActions.ACTION_VOLUME_UP to R.drawable.action_volume_up,
            StandardActions.ACTION_VOLUME_DOWN to R.drawable.action_volume_down,
            StandardActions.ACTION_OPEN_MENU to R.drawable.action_open_menu
    )

    fun hasIcon(key: String): Boolean = iconMap.containsKey(key)
    fun getIcon(key: String): Int = iconMap[key] ?: 0
}