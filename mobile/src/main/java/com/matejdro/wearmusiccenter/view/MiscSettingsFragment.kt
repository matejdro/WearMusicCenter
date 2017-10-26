package com.matejdro.wearmusiccenter.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearutils.logging.LogRetrievalTask
import com.matejdro.wearutils.preferencesync.PreferencePusher
import de.psdev.licensesdialog.LicensesDialog


class MiscSettingsFragment : PreferenceFragment() {
    private lateinit var googleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)

        findPreference("supportButton").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            sendLogs()
            true
        }

        try {
            findPreference("version").summary =
                    activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
        } catch (ignored: PackageManager.NameNotFoundException) {
        }

        findPreference("donateButton").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=JAFYXYH2PDRAW")
            startActivity(intent)
            true
        }

        findPreference("licenses").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            LicensesDialog.Builder(activity)
                    .setNotices(R.raw.notices)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show()
            true
        }

        googleApiClient = GoogleApiClient.Builder(activity)
                .addApi(Wearable.API)
                .build()

        googleApiClient.connect()
    }

    private fun sendLogs() {
        LogRetrievalTask(activity,
                CommPaths.MESSAGE_SEND_LOGS,
                "matejdro+support@gmail.com",
                "com.matejdro.wearmusiccenter.logs").execute(null as Void?)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (permissions.isNotEmpty() &&
                permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendLogs()
        }
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
