package com.matejdro.wearmusiccenter.logging

import com.crashlytics.android.Crashlytics

import timber.log.Timber

class TimberCrashlytics : Timber.Tree() {
    override fun log(priority: Int, tag: String, message: String, t: Throwable?) {
        if (t != null) {
            Crashlytics.log(priority, tag, message)
            Crashlytics.logException(t)
        }
    }
}
