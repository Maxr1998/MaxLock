/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2018  Max Rumpf alias Maxr1998
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.Maxr1998.xposed.maxlock.ui.settings.applist

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.haibison.android.lockpattern.LockPatternActivity
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.MLImplementation
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.settings.LockSetupFragment
import de.Maxr1998.xposed.maxlock.ui.settings.MaxLockPreferenceFragment
import de.Maxr1998.xposed.maxlock.util.KUtil.getPatternCode
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.Util
import de.Maxr1998.xposed.maxlock.util.asReference
import java.util.*

class AppListAdapter(val appListModel: AppListModel, context: Context) : RecyclerView.Adapter<AppListViewHolder>(), Filterable {
    private val prefs = MLPreferences.getPreferences(context)
    private val prefsApps = MLPreferences.getPrefsApps(context)
    private var prefsKeysPerApp = MLPreferences.getPreferencesKeysPerApp(context)
    private val filter = AppFilter(this, prefs, prefsApps)

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.app_list_item, parent, false)
        return AppListViewHolder(v)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        val appInfo = appListModel.appList[position]
        holder.bind(appInfo, appListModel.iconCache)
        holder.options.setOnClickListener {
            var items = it.resources.getTextArray(R.array.dialog_multi_select_items_options)
            if (MLImplementation.getImplementation(prefs) != MLImplementation.DEFAULT)
                items = Arrays.copyOf<CharSequence>(items, items.size - 2)
            val optionsDialog = AlertDialog.Builder(it.context)
                    .setTitle(it.resources.getString(R.string.dialog_title_options))
                    .setIcon(appListModel.iconCache[appInfo.packageName] ?: appInfo.loadIcon())
                    .setMultiChoiceItems(items, booleanArrayOf(
                            prefsKeysPerApp.contains(appInfo.packageName),
                            prefsApps.getBoolean(appInfo.packageName + Common.APP_FAKE_CRASH_PREFERENCE, false),
                            prefsApps.getBoolean(appInfo.packageName + Common.APP_HIDE_NOTIFICATIONS_PREFERENCE, false),
                            prefsApps.getBoolean(appInfo.packageName + Common.APP_HIDE_NOTIFICATION_CONTENT_PREFERENCE, false)))
                    { dialog, which, isChecked ->
                        var prefKey: String? = null
                        when (which) {
                            0 -> {
                                handleCustomLockingType(it.context, appInfo.packageName, isChecked, dialog, holder.adapterPosition)
                                return@setMultiChoiceItems
                            }
                            1 -> prefKey = appInfo.packageName + Common.APP_FAKE_CRASH_PREFERENCE
                            2 -> prefKey = appInfo.packageName + Common.APP_HIDE_NOTIFICATIONS_PREFERENCE
                            3 -> prefKey = appInfo.packageName + Common.APP_HIDE_NOTIFICATION_CONTENT_PREFERENCE
                        }
                        prefKey?.let { key ->
                            prefsApps.edit {
                                if (isChecked) putBoolean(key, true)
                                else remove(key)
                            }
                        }
                    }
                    .setPositiveButton(android.R.string.ok, null)
            if (MLImplementation.getImplementation(MLPreferences.getPreferences(it.context)) == MLImplementation.DEFAULT)
                optionsDialog.setNeutralButton(R.string.dialog_button_exclude_activities) { _, _ ->
                    showActivities(appListModel, it.context.asReference(), appInfo.packageName)
                }
            optionsDialog.create().let { dialog -> appListModel.dialogDispatcher.call(dialog) }
        }
    }

    override fun onViewRecycled(holder: AppListViewHolder) {
        holder.appIcon.setImageDrawable(null)
    }

    override fun getItemId(position: Int) = appListModel.appList[position].id.toLong()

    override fun getItemCount() = appListModel.appList.size()

    override fun getItemViewType(position: Int) = R.layout.app_list_item

    override fun getFilter(): Filter = filter

    @Suppress("unused")
    fun getSectionName(position: Int): String {
        fun transformLetter(c: Char): String {
            return if (c.isDigit()) {
                "#"
            } else c.toString()
        }
        return appListModel.appList.run {
            val pos = if (position < size()) position else if (size() > 0) size() - 1 else 0
            transformLetter(get(pos).name[0])
        }
    }

    private fun handleCustomLockingType(context: Context, packageName: String, checked: Boolean, dialog: DialogInterface, adapterPosition: Int) {
        if (checked) {
            dialog.dismiss()
            AlertDialog.Builder(context)
                    .setItems(arrayOf(
                            context.getString(R.string.pref_locking_type_password),
                            context.getString(R.string.pref_locking_type_pin),
                            context.getString(R.string.pref_locking_type_knockcode),
                            context.getString(R.string.pref_locking_type_pattern)))
                    { typeDialog, i ->
                        typeDialog.dismiss()
                        val extras = Bundle(2)
                        when (i) {
                            0 -> {
                                Util.setPassword(context, packageName)
                                return@setItems
                            }
                            1 -> extras.putString(Common.LOCKING_TYPE, Common.LOCKING_TYPE_PIN)
                            2 -> extras.putString(Common.LOCKING_TYPE, Common.LOCKING_TYPE_KNOCK_CODE)
                            3 -> {
                                val intent = Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, context, LockPatternActivity::class.java)
                                appListModel.fragmentFunctionDispatcher.value = {
                                    startActivityForResult(intent, getPatternCode(adapterPosition))
                                }
                                return@setItems
                            }
                        }
                        extras.putString(Common.INTENT_EXTRAS_CUSTOM_APP, packageName)
                        LockSetupFragment().let {
                            it.arguments = extras
                            appListModel.fragmentFunctionDispatcher.value = {
                                MaxLockPreferenceFragment.launchFragment(this, it, false)
                            }
                        }
                    }.create().let { appListModel.dialogDispatcher.call(it) }
        } else {
            prefsKeysPerApp.edit {
                remove(packageName)
                remove(packageName + Common.APP_KEY_PREFERENCE)
            }
        }
    }
}