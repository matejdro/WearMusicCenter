package com.matejdro.wearvibrationcenter.notificationprovider;

import android.content.ComponentName;

public class NotificationProviderConstants {
    public static final String ACTION_NOTIFICATION_PROVIDER = "PROVIDE_NOTIFICATIONS";
    public static final ComponentName TARGET_COMPONENT = new ComponentName(
            "com.matejdro.wearvibrationcenter",
            "com.matejdro.wearvibrationcenter.notificationprovider.NotificationBroadcastMediator"
    );
}
