package com.matejdro.wearmusiccenter.common;

import com.matejdro.wearutils.preferences.definition.PreferenceDefinition;
import com.matejdro.wearutils.preferences.definition.SimplePreferenceDefinition;

public class MiscPreferences {
    public static final PreferenceDefinition<Boolean> ALWAYS_SHOW_TIME
            = new SimplePreferenceDefinition<>("always_show_time", false);
}