package com.matejdro.wearmusiccenter.view.settings

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearutils.preferences.compat.PreferenceWithDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicAppListPreference
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null) :
    DialogPreference(context, attributeSet), PreferenceWithDialog {
    var appList: List<App>? = null

    var blacklist: MutableSet<String>
        get() = getPersistedStringSet(emptySet())
        set(value) {
            persistStringSet(value)
        }

    override fun createDialog(key: String): PreferenceDialogFragmentCompat {
        return MusicAppListPreferenceDialog.create(key)
    }

    override fun onClick() {
        GlobalScope.launch(Dispatchers.Main) {

            @Suppress("DEPRECATION")
            val progressDialog = android.app.ProgressDialog(this@MusicAppListPreference.context!!)
            progressDialog.setMessage(this@MusicAppListPreference.context!!.getString(R.string.please_wait))
            progressDialog.show()

            appList = getAllApps()

            progressDialog.hide()
            preferenceManager.showDialog(this@MusicAppListPreference)
        }
    }

    private suspend fun getAllApps(): List<App> = withContext(Dispatchers.Default) {
        val packageManager = this@MusicAppListPreference.context!!.packageManager

        packageManager.getInstalledPackages(0)
            .map {
                val appLabel = try {
                    val appInfo = packageManager.getApplicationInfo(it.packageName, 0)
                    packageManager.getApplicationLabel(appInfo)
                } catch (e: PackageManager.NameNotFoundException) {
                    it.packageName
                }

                App(it.packageName, appLabel.toString())
            }
            .sortedBy { it.label }
    }

    class MusicAppListPreferenceDialog : PreferenceDialogFragmentCompat() {
        companion object {
            fun create(key: String): MusicAppListPreferenceDialog {
                val fragment = MusicAppListPreferenceDialog()

                val arguments = Bundle(1)
                arguments.putString(ARG_KEY, key)

                fragment.arguments = arguments
                return fragment
            }
        }

        private lateinit var installedApps: List<App>
        private lateinit var selectedApps: BooleanArray

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            installedApps = (preference as MusicAppListPreference).appList ?: emptyList()

            val savedBlacklist = (preference as MusicAppListPreference).blacklist
            selectedApps =
                BooleanArray(installedApps.size) { !savedBlacklist.contains(installedApps[it].pkg) }

            return super.onCreateDialog(savedInstanceState)
        }

        override fun onDialogClosed(positiveResult: Boolean) {
            if (!positiveResult) {
                return
            }

            val newBlacklist = HashSet<String>(installedApps
                .filterIndexed { index, _ -> !selectedApps[index] }
                .map { it.pkg })

            (preference as MusicAppListPreference).blacklist = newBlacklist
        }

        override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
            builder.setMultiChoiceItems(
                installedApps.map { it.label }.toTypedArray(),
                selectedApps
            ) { _, which, isChecked ->
                selectedApps[which] = isChecked
            }
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
        }

        override fun onCreateDialogView(context: Context?): View? {
            return null
        }
    }

    data class App(val pkg: String, val label: String)
}
