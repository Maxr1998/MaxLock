/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2016 Max Rumpf alias Maxr1998
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

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.edit
import com.haibison.android.lockpattern.LockPatternActivity
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.MLImplementation
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.settings.LockSetupFragment
import de.Maxr1998.xposed.maxlock.ui.settings.MaxLockPreferenceFragment
import de.Maxr1998.xposed.maxlock.util.KUtil.getPatternCode
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.Util
import de.Maxr1998.xposed.maxlock.util.asReference
import de.Maxr1998.xposed.maxlock.util.showWithLifecycle
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.util.*

class AppListAdapter(val fragment: Fragment) : RecyclerView.Adapter<AppListViewHolder>(), Filterable, FastScrollRecyclerView.SectionedAdapter {
    val appListModel: AppListModel by lazy { ViewModelProviders.of(fragment.activity!!).get(AppListModel::class.java) }
    private val prefs = MLPreferences.getPreferences(fragment.activity)
    private val prefsApps = MLPreferences.getPrefsApps(fragment.activity)
    private var prefsKeysPerApp = MLPreferences.getPreferencesKeysPerApp(fragment.activity)
    private val filter = AppFilter(this, prefs, prefsApps)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.app_list_item, parent, false)
        return AppListViewHolder(v)
    }

    @SuppressLint("ApplySharedPref")
    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        val appInfo = appListModel.appList[position]
        holder.bind(appInfo)
        holder.appIcon.setImageDrawable(null)
        async(UI) {
            val iconRef = holder.appIcon.asReference()
            val drawable = async { appInfo.loadIcon(appListModel.iconCache) }
            iconRef.invoke().setImageDrawable(drawable.await())
        }
        holder.options.setOnClickListener {
            var items = it.resources.getTextArray(R.array.dialog_multi_select_items_options)
            if (MLImplementation.getImplementation(prefs) != MLImplementation.DEFAULT)
                items = Arrays.copyOf<CharSequence>(items, items.size - 2)
            val optionsDialog = AlertDialog.Builder(it.context)
                    .setTitle(it.resources.getString(R.string.dialog_title_options))
                    .setIcon(appInfo.loadIcon(appListModel.iconCache))
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
                        prefKey?.let {
                            prefsApps.edit(true) {
                                if (isChecked) putBoolean(it, true)
                                else remove(it)
                            }
                        }
                    }
                    .setPositiveButton(android.R.string.ok, null)
            if (MLImplementation.getImplementation(MLPreferences.getPreferences(it.context)) == MLImplementation.DEFAULT)
                optionsDialog.setNeutralButton(R.string.dialog_button_exclude_activities) { _, _ ->
                    showActivities(fragment.asReference(), it.context.asReference(), appInfo.packageName)
                }
            optionsDialog.create().showWithLifecycle(fragment)
        }
    }

    override fun getItemCount(): Int {
        return appListModel.appList.size
    }

    override fun getFilter(): Filter = filter

    override fun getSectionName(position: Int): String {
        fun transformLetter(c: Char): String {
            return if (c.isDigit()) {
                "#"
            } else c.toString()
        }
        return appListModel.appList.run {
            val pos = if (position < size) position else if (size > 0) size - 1 else 0
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
                                fragment.startActivityForResult(intent, getPatternCode(adapterPosition))
                                return@setItems
                            }
                        }
                        extras.putString(Common.INTENT_EXTRAS_CUSTOM_APP, packageName)
                        LockSetupFragment().let {
                            it.arguments = extras
                            MaxLockPreferenceFragment.launchFragment(fragment, it, false)
                        }
                    }.create().showWithLifecycle(fragment)
        } else {
            prefsKeysPerApp.edit {
                remove(packageName)
                remove(packageName + Common.APP_KEY_PREFERENCE)
            }
        }
    }
}