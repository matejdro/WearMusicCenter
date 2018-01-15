package com.matejdro.wearmusiccenter.view.settings

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.PreferenceDialogFragmentCompat
import android.util.AttributeSet
import android.view.View
import com.matejdro.wearmusiccenter.actions.appplay.AppPlayPickerAction
import com.matejdro.wearutils.preferences.compat.PreferenceWithDialog

class MusicAppListPreference
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null) : DialogPreference(context, attributeSet), PreferenceWithDialog {

    var blacklist: MutableSet<String>
        get() = getPersistedStringSet(emptySet())
        set(value) {
            persistStringSet(value)
        }

    override fun createDialog(key: String): PreferenceDialogFragmentCompat {
        return MusicAppListPreferenceDialog.create(key)
    }

    class MusicAppListPreferenceDialog : PreferenceDialogFragmentCompat() {
        companion object {
            fun create(key: String): MusicAppListPreferenceDialog {
                val fragment = MusicAppListPreferenceDialog()

                val arguments = Bundle(1)
                arguments.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)

                fragment.arguments = arguments
                return fragment
            }
        }

        private lateinit var installedApps: List<App>
        private lateinit var selectedApps: BooleanArray

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val packageManager = context!!.packageManager

            installedApps = AppPlayPickerAction.getAllMusicApps(context!!)
                    .distinctBy { it.packageName }
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

            val savedBlacklist = (preference as MusicAppListPreference).blacklist
            selectedApps = BooleanArray(installedApps.size) { !savedBlacklist.contains(installedApps[it].pkg) }

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
            builder.setMultiChoiceItems(installedApps.map { it.label }.toTypedArray(), selectedApps) { _, which, isChecked ->
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