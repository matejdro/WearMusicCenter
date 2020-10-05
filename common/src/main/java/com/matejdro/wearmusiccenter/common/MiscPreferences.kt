package com.matejdro.wearmusiccenter.common

import com.matejdro.wearutils.preferences.definition.PreferenceDefinition
import com.matejdro.wearutils.preferences.definition.SimplePreferenceDefinition

object MiscPreferences {
    val ALWAYS_SHOW_TIME: PreferenceDefinition<Boolean>
            = SimplePreferenceDefinition("always_show_time", false)

    val PAUSE_ON_SWIPE_EXIT: PreferenceDefinition<Boolean>
            = SimplePreferenceDefinition("pause_on_swipe_exit", false)

    val ROTATING_CROWN_OFF_PERIOD: PreferenceDefinition<Int>
            = SimplePreferenceDefinition("rotating_crown_off_period", 300)

    val HAPTIC_FEEDBACK: PreferenceDefinition<Boolean>
            = SimplePreferenceDefinition("haptic_feedback", true)

    val DISABLE_PHYSICAL_DOUBLE_CLICK_IN_AMBIENT: PreferenceDefinition<Boolean> = SimplePreferenceDefinition("disable_ambient_physical_double_click", false)

    val AUTO_START: PreferenceDefinition<Boolean>
            = SimplePreferenceDefinition("auto_start", false)

    val AUTO_START_APP_BLACKLIST: PreferenceDefinition<Set<String>>
            = SimplePreferenceDefinition("auto_start_apps_blacklist", emptySet())

    val CLOSE_TIMEOUT: PreferenceDefinition<Int>
            = SimplePreferenceDefinition("close_timeout", 0)

    val ENABLE_NOTIFICATION_POPUP: PreferenceDefinition<Boolean>
            = SimplePreferenceDefinition("enable_notification_popup", false)

    val NOTIFICATION_TIMEOUT: PreferenceDefinition<Int>
            = SimplePreferenceDefinition("notification_timeout", 10)

    val ALWAYS_SELECT_CENTER_ACTION: PreferenceDefinition<Boolean>
            = SimplePreferenceDefinition("always_select_center_action", false)

    val LAST_MENU_DISPLAYED: PreferenceDefinition<String>
            = SimplePreferenceDefinition("last_menu_displayed", "-1")

    val OPEN_PLAYBACK_QUEUE_ON_SWIPE_UP: PreferenceDefinition<Boolean>
            = SimplePreferenceDefinition("swipe_up_for_queue", false)
}