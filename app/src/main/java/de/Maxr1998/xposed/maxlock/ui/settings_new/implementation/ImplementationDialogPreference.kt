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

package de.Maxr1998.xposed.maxlock.ui.settings_new.implementation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.View
import de.Maxr1998.modernpreferences.preferences.DialogPreference
import de.Maxr1998.xposed.maxlock.R

class ImplementationDialogPreference : DialogPreference("implementation") {

    init {
        titleRes = R.string.ml_implementation
    }

    @SuppressLint("InflateParams")
    override fun createDialog(context: Context): Dialog {
        return AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setView(View.inflate(context, R.layout.dialog_implementation, null))
                .setNegativeButton(android.R.string.ok, null)
                .setOnDismissListener { requestRebind() }
                .create()
    }
}