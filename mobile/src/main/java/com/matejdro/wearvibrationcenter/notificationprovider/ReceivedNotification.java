package com.matejdro.wearvibrationcenter.notificationprovider;

import android.os.Parcel;
import android.os.Parcelable;

public class ReceivedNotification implements Parcelable {
    private final String title;
    private final String description;
    private final byte[] imageDataPng;

    public ReceivedNotification(String title, String description, byte[] imageDataPng) {
        this.title = title;
        this.description = description;
        this.imageDataPng = imageDataPng;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public byte[] getImageDataPng() {
        return imageDataPng;
    }

    protected ReceivedNotification(Parcel in) {
        title = in.readString();
        description = in.readString();
        imageDataPng = in.createByteArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(description);
        dest.writeByteArray(imageDataPng);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ReceivedNotification> CREATOR = new Creator<ReceivedNotification>() {
        @Override
        public ReceivedNotification createFromParcel(Parcel in) {
            return new ReceivedNotification(in);
        }

        @Override
        public ReceivedNotification[] newArray(int size) {
            return new ReceivedNotification[size];
        }
    };
}
