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

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.widget.Filter
import java.util.*

class AppFilter(private val adapter: AppListAdapter, private val prefs: SharedPreferences, private val prefsApps: SharedPreferences) : Filter() {
    @SuppressLint("DefaultLocale")
    override fun performFiltering(filter: CharSequence): Filter.FilterResults? {
        val backup = adapter.appListModel.appListBackup
        val search = filter.toString().toLowerCase()
        val defaultFilter = prefs.getString("app_list_filter", "")
        if (search.isEmpty() && defaultFilter.isNullOrEmpty()) {
            return null
        } else {
            val filteredList = ArrayList<AppListModel.AppInfo>()
            for (i in backup.indices) {
                var add = false
                if (search.isNotEmpty()) {
                    if (search == backup[i].packageName.toLowerCase()) {
                        add = true
                    } else {
                        val title = backup[i].name.toLowerCase()
                        if (title.startsWith(search)) {
                            add = true
                        }
                        // Spaces/multiple words in title
                        if (!add)
                            for (titlePart in title.split(" ".toRegex())) {
                                if (titlePart.startsWith(search)) {
                                    add = true
                                    break
                                }
                            }
                        // Spaces/multiple words in search
                        if (!add)
                            for (searchPart in search.split(" ".toRegex())) {
                                if (title.startsWith(searchPart)) {
                                    add = true
                                    break
                                }
                            }
                    }
                } else {
                    add = true
                }
                val isEnabled = prefsApps.getBoolean(backup[i].packageName, false)
                if (add && (defaultFilter == "" || isEnabled && defaultFilter == "@*activated*" || !isEnabled && defaultFilter == "@*deactivated*")) {
                    filteredList.add(backup[i])
                }
            }
            return FilterResults().apply {
                values = filteredList
                count = filteredList.size
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun publishResults(constraint: CharSequence, results: Filter.FilterResults?) {
        adapter.appListModel.appList.apply {
            val resultList = if (results != null) {
                results.values as ArrayList<AppListModel.AppInfo>
            } else adapter.appListModel.appListBackup
            if (size() != resultList.size) {
                replaceAll(resultList)
            }
        }
    }
}