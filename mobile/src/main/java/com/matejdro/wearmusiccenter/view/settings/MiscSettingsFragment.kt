package com.matejdro.wearmusiccenter.view.settings

import android.Manifest
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Wearable
import com.matejdro.wearmusiccenter.NotificationService
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearutils.logging.LogRetrievalTask
import com.matejdro.wearutils.preferences.compat.PreferenceFragmentCompatEx
import com.matejdro.wearutils.preferencesync.PreferencePusher
import de.psdev.licensesdialog.LicensesDialog


class MiscSettingsFragment : PreferenceFragmentCompatEx(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        private const val VIBRATION_CENTER_PACKAGE = "com.matejdro.wearvibrationcenter"
    }

    private lateinit var googleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        googleApiClient = GoogleApiClient.Builder(context!!)
                .addApi(Wearable.API)
                .build()

        googleApiClient.connect()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)

        initAutomationSection()
        initNotificationsSection()
        initAboutSection()
    }

    private fun initAutomationSection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // On Android N and above we unbind notification service when autostart is disabled and
            // rebind when enabled

            findPreference<Preference>("auto_start")!!.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        newValue as Boolean

                        if (newValue) {
                            NotificationListenerService.requestRebind(
                                    ComponentName(context!!, NotificationService::class.java)
                            )
                        } else {
                            val serviceStopIntent = Intent(context!!, NotificationService::class.java)
                            serviceStopIntent.action = NotificationService.ACTION_UNBIND_SERVICE
                            context!!.startService(serviceStopIntent)
                        }

                        true
                    }
        }
    }

    private fun initNotificationsSection() {
        findPreference<Preference>("enable_notification_popup")!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, value: Any ->
            if (value == false) {
                return@OnPreferenceChangeListener true
            }

            if (!isVibrationCenterInstalledAndEnabled()) {
                AlertDialog.Builder(context!!)
                        .setTitle(R.string.app_required)
                        .setMessage(R.string.vibration_center_required_description)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.open_play_store) { _: DialogInterface, _: Int -> openVibrationCenterPlayStore() }
                        .show()

                return@OnPreferenceChangeListener false
            }

            true
        }
    }

    private fun initAboutSection() {
        findPreference<Preference>("supportButton")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            sendLogs()
            true
        }

        try {
            findPreference<Preference>("version")!!.summary =
                    activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName
        } catch (ignored: PackageManager.NameNotFoundException) {
        }

        findPreference<Preference>("licenses")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            LicensesDialog.Builder(activity)
                    .setNotices(R.raw.notices)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show()
            true
        }
    }

    private fun isVibrationCenterInstalledAndEnabled(): Boolean {
        try {
            val appInfo = context!!.packageManager.getApplicationInfo(VIBRATION_CENTER_PACKAGE, 0)
            return appInfo.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }

    private fun openVibrationCenterPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + VIBRATION_CENTER_PACKAGE)))
        } catch (_: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + VIBRATION_CENTER_PACKAGE)))
        }
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

    override fun onStart() {
        super.onStart()

        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()

        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        pushPreferencesToWatch()
    }

    private fun pushPreferencesToWatch() {
        if (googleApiClient.isConnected) {
            PreferencePusher.pushPreferences(googleApiClient, preferenceManager.sharedPreferences, CommPaths.PREFERENCES_PREFIX, false)
        }
    }

    override fun onDestroy() {
        googleApiClient.disconnect()

        super.onDestroy()
    }
}
