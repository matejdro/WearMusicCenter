package com.matejdro.wearmusiccenter.common;

import com.matejdro.wearutils.preferences.definition.PreferenceDefinition;
import com.matejdro.wearutils.preferences.definition.SimplePreferenceDefinition;

public class MiscPreferences {
    public static final PreferenceDefinition<Boolean> ALWAYS_SHOW_TIME
            = new SimplePreferenceDefinition<>("always_show_time", false);

    public static final PreferenceDefinition<Boolean> PAUSE_ON_SWIPE_EXIT
            = new SimplePreferenceDefinition<>("pause_on_swipe_exit", false);

}