package com.matejdro.wearmusiccenter.logging

import com.google.android.gms.wearable.DataMap
import com.google.firebase.crashlytics.FirebaseCrashlytics

import pl.tajchert.exceptionwear.ExceptionWearHandler
import timber.log.Timber

class CrashlyticsExceptionWearHandler : ExceptionWearHandler {
    override fun handleException(throwable: Throwable, map: DataMap) {
        Timber.d("HandleException %s", throwable)
        FirebaseCrashlytics.getInstance().setCustomKey("wear_exception", true)
        FirebaseCrashlytics.getInstance().setCustomKey("board", map.getString("board")!!)
        FirebaseCrashlytics.getInstance()
            .setCustomKey("fingerprint", map.getString("fingerprint")!!)
        FirebaseCrashlytics.getInstance().setCustomKey("model", map.getString("model")!!)
        FirebaseCrashlytics.getInstance()
            .setCustomKey("manufacturer", map.getString("manufacturer")!!)
        FirebaseCrashlytics.getInstance().setCustomKey("product", map.getString("product")!!)
        FirebaseCrashlytics.getInstance().setCustomKey(
            "api_level",
            map.getString("api_level")!!
        )

        FirebaseCrashlytics.getInstance().recordException(throwable)
    }
}