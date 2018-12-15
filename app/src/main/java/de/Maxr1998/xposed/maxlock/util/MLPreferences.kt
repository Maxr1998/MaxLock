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

package de.Maxr1998.xposed.maxlock.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.preference.PreferenceManager
import de.Maxr1998.xposed.maxlock.Common.*

object MLPreferences {
    private var prefs: SharedPreferences? by SoftReferenceDelegate()
    private var prefsApps: SharedPreferences? by SoftReferenceDelegate()
    private var prefsHistory: SharedPreferences? by SoftReferenceDelegate()
    private var prefsKey: SharedPreferences? by SoftReferenceDelegate()
    private var prefsKeysPerApp: SharedPreferences? by SoftReferenceDelegate()

    @JvmStatic
    fun getPreferences(context: Context): SharedPreferences {
        return prefs ?: PreferenceManager.getDefaultSharedPreferences(context).apply {
            prefs = this
        }
    }

    @JvmStatic
    fun getPrefsApps(context: Context): SharedPreferences {
        return prefsApps ?: context.getSharedPreferences(PREFS_APPS, MODE_PRIVATE).apply {
            prefsApps = this
        }
    }

    @JvmStatic
    fun getPrefsHistory(context: Context): SharedPreferences {
        return prefsHistory ?: context.getSharedPreferences(PREFS_HISTORY, MODE_PRIVATE).apply {
            prefsHistory = this
        }
    }

    @JvmStatic
    fun getPreferencesKeys(context: Context): SharedPreferences {
        return prefsKey ?: context.getSharedPreferences(PREFS_KEY, MODE_PRIVATE).apply {
            prefsKey = this
        }
    }

    @JvmStatic
    fun getPreferencesKeysPerApp(context: Context): SharedPreferences {
        return prefsKeysPerApp ?: context.getSharedPreferences(PREFS_KEYS_PER_APP, MODE_PRIVATE)
                .apply { prefsKeysPerApp = this }
    }
}

inline val Context.prefs: SharedPreferences
    get() = MLPreferences.getPreferences(this)
inline val Context.prefsApps: SharedPreferences
    get() = MLPreferences.getPrefsApps(this)
inline val Context.prefsKey: SharedPreferences
    get() = MLPreferences.getPreferencesKeys(this)
inline val Context.prefsKeysPerApp: SharedPreferences
    get() = MLPreferences.getPreferencesKeysPerApp(this)