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

import androidx.recyclerview.widget.SortedList
import java.text.Collator

class AppListCallback(private val adapter: AppListAdapter) : SortedList.Callback<AppListModel.AppInfo>() {

    private val collator = Collator.getInstance()

    override fun areItemsTheSame(old: AppListModel.AppInfo, new: AppListModel.AppInfo) =
            old.id == new.id

    override fun areContentsTheSame(old: AppListModel.AppInfo, new: AppListModel.AppInfo) =
            old.packageName == new.packageName

    override fun compare(a: AppListModel.AppInfo, b: AppListModel.AppInfo) =
            collator.compare(a.name, b.name)

    override fun onInserted(position: Int, count: Int) {
        adapter.notifyItemRangeInserted(position, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        adapter.notifyItemRangeRemoved(position, count)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        adapter.notifyItemMoved(fromPosition, toPosition)
    }

    override fun onChanged(position: Int, count: Int) {
        adapter.notifyItemRangeChanged(position, count)
    }
}