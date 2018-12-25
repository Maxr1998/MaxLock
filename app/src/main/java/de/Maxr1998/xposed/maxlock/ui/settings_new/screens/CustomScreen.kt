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

package de.Maxr1998.xposed.maxlock.ui.settings_new.screens

import android.view.Menu
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import de.Maxr1998.xposed.maxlock.util.GenericEventLiveData

abstract class CustomScreen {
    abstract val viewModel: ViewModel

    @StringRes
    open val titleRes = -1
    open val hasOptionsMenu = false

    open val adapter: RecyclerView.Adapter<*>? = null
    open val view: View? = null

    /**
     * @return a [GenericEventLiveData] if data is currently being loaded, else null
     */
    open val progressLiveData: GenericEventLiveData<Any?>? = null

    open fun createOptionsMenu(menu: Menu) {}
    open fun prepareOptionsMenu(menu: Menu) {}
}