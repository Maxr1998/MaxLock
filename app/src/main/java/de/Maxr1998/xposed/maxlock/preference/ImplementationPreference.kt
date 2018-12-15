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

package de.Maxr1998.xposed.maxlock.preference

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import de.Maxr1998.xposed.maxlock.R

class ImplementationPreference(context: Context, attr: AttributeSet) : Preference(context, attr) {

    private var warningVisible = false
    private var statusIcon: View? = null

    init {
        widgetLayoutResource = R.layout.preference_widget_implementation
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        statusIcon = view.findViewById(R.id.status_icon)
        updateView()
    }

    fun setWarningVisible(visible: Boolean) {
        warningVisible = visible
        updateView()
    }

    private fun updateView() {
        statusIcon?.visibility = if (warningVisible) View.VISIBLE else View.GONE
    }
}