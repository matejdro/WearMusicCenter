package com.matejdro.wearmusiccenter.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class TimberCrashlytics : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
        if (t != null) {
            message?.let { FirebaseCrashlytics.getInstance().log(it) }
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}
