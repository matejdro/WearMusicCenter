// INotificationProvider.aidl
package com.matejdro.wearvibrationcenter.notificationprovider;

import com.matejdro.wearvibrationcenter.notificationprovider.INotificationListener;

interface INotificationProvider {
    void startSendingNotifications(INotificationListener listener);
}
