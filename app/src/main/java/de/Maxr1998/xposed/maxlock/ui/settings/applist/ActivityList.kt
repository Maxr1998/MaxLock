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
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.Ref
import de.Maxr1998.xposed.maxlock.util.inflate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

fun showActivities(appListModel: AppListModel, context: Ref<Context>, packageName: String) {
    CoroutineScope(Dispatchers.Main).launch {
        val activities = withContext(Dispatchers.IO) {
            ArrayList<String>().apply {
                try {
                    val activities = context().packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities
                    for (info in activities) {
                        add(info.name)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
                sort()
            }
        }
        val ctx = context()
        val recyclerView = RecyclerView(ctx)
        val adapter = ActivityListAdapter(ctx, activities)
        recyclerView.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(ctx)
            itemAnimator = DefaultItemAnimator()
        }
        appListModel.dialogDispatcher.value = AlertDialog.Builder(ctx)
                .setTitle(R.string.dialog_title_exclude_activities)
                .setView(recyclerView)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.dialog_button_invert_activities, null).create().apply {
                    setOnShowListener { _ ->
                        getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener { _ -> adapter.invert() }
                    }
                }
    }
}

@SuppressLint("ApplySharedPref")
private class ActivityListAdapter internal constructor(context: Context, private val activities: List<String>) : RecyclerView.Adapter<ActivityListViewHolder>() {
    private val prefsApps: SharedPreferences = MLPreferences.getPrefsApps(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ActivityListViewHolder(parent.inflate(R.layout.list_activities_item))

    override fun onBindViewHolder(lvh: ActivityListViewHolder, position: Int) {
        val name = activities[lvh.layoutPosition]
        lvh.switchCompat.isChecked = prefsApps.getBoolean(name, true)
        lvh.switchCompat.text = name
        lvh.switchCompat.setOnCheckedChangeListener { _, b ->
            val now = activities[lvh.layoutPosition]
            prefsApps.edit(true) {
                if (b) remove(now)
                else putBoolean(now, false)
            }
        }
    }

    override fun getItemCount(): Int {
        return activities.size
    }

    fun invert() {
        prefsApps.edit(true) {
            for (i in activities.indices) {
                val s = activities[i]
                if (prefsApps.getBoolean(s, true)) putBoolean(s, false)
                else remove(s)
            }
        }
        notifyDataSetChanged()
    }
}

private class ActivityListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val switchCompat: SwitchCompat = itemView.findViewById<SwitchCompat>(R.id.activity_switch).apply {
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }
}