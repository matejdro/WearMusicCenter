package com.matejdro.wearmusiccenter.view

import android.os.Bundle
import android.preference.PreferenceFragment
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearutils.preferencesync.PreferencePusher


class MiscSettingsFragment : PreferenceFragment() {
    private lateinit var googleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)

        googleApiClient = GoogleApiClient.Builder(activity)
                .addApi(Wearable.API)
                .build()

        googleApiClient.connect()
    }

    override fun onStop() {
        super.onStop()

        if (googleApiClient.isConnected) {
            PreferencePusher.pushPreferences(googleApiClient, preferenceManager.sharedPreferences, CommPaths.PREFERENCES_PREFIX, false)
        }
    }

    override fun onDestroy() {
        googleApiClient.disconnect()

        super.onDestroy()
    }
}
