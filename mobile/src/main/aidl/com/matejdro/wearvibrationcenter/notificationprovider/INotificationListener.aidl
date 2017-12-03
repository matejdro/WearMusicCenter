// INotificationListener.aidl
package com.matejdro.wearvibrationcenter.notificationprovider;

import com.matejdro.wearvibrationcenter.notificationprovider.ReceivedNotification;

interface INotificationListener {
    oneway void onNotificationReceived(in ReceivedNotification notification);
}
