package com.matejdro.wearmusiccenter.logging

import com.crashlytics.android.Crashlytics
import com.google.android.gms.wearable.DataMap

import pl.tajchert.exceptionwear.ExceptionWearHandler
import timber.log.Timber

class CrashlyticsExceptionWearHandler : ExceptionWearHandler {
    override fun handleException(throwable: Throwable, map: DataMap) {
        Timber.d("HandleException %s", throwable)
        Crashlytics.setBool("wear_exception", true)
        Crashlytics.setString("board", map.getString("board"))
        Crashlytics.setString("fingerprint", map.getString("fingerprint"))
        Crashlytics.setString("model", map.getString("model"))
        Crashlytics.setString("manufacturer", map.getString("manufacturer"))
        Crashlytics.setString("product", map.getString("product"))
        Crashlytics.setString("api_level", map.getString("api_level"))

        Crashlytics.logException(throwable)
    }
}