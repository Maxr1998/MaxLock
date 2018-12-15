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

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import de.Maxr1998.xposed.maxlock.Common
import org.json.JSONArray
import org.json.JSONObject

object AppLockHelpers {

    private const val PROCESS_HISTORY_ARRAY = "procs"
    private const val PACKAGE_HISTORY_ARRAY = "pkgs"
    private const val UNLOCK_ID = -0x3A8
    private const val IMOD_APPS = "iModPerApp"
    private const val CLOSE_APPS = "close"
    private const val IMOD_LAST_UNLOCK_GLOBAL = "IMoDGlobalDelayTimer"
    const val IMOD_RESET_ON_SCREEN_OFF = "reset_imod_screen_off"
    const val IMOD_RESET_ON_HOMESCREEN = "imod_reset_on_homescreen"
    private const val IMOD_DELAY_APP = "delay_inputperapp"
    private const val IMOD_DELAY_GLOBAL = "delay_inputgeneral"

    @JvmStatic
    @Throws(Throwable::class)
    fun pass(taskId: Int, packageName: String, activityName: String?, prefsApps: SharedPreferences, prefsHistory: SharedPreferences): Boolean {
        addToHistory(taskId, packageName, prefsHistory)
        // MasterSwitch disabled
        if (!prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)) {
            return true
        }

        // Activity got launched/closed
        if (JSONArray(prefsHistory.getString(PROCESS_HISTORY_ARRAY, JSONArray().toString())).optInt(1) == UNLOCK_ID) {
            return true
        }

        // Activity not locked
        activityName?.let {
            if (!prefsApps.getBoolean(it, true))
                return true
        }

        // I.Mod active
        return iModActive(packageName, prefsApps, prefsHistory)
    }

    @JvmStatic
    fun iModActive(packageName: String, prefsApps: SharedPreferences, prefsHistory: SharedPreferences): Boolean {
        val iModDelayGlobalEnabled = prefsApps.getBoolean(Common.ENABLE_IMOD_DELAY_GLOBAL, false)
        val iModDelayAppEnabled = prefsApps.getBoolean(Common.ENABLE_IMOD_DELAY_APP, false)
        val iModLastUnlockGlobal = prefsHistory.getLong(IMOD_LAST_UNLOCK_GLOBAL, 0)
        val iModPerApp = JSONObject(prefsHistory.getString(IMOD_APPS, JSONObject().toString()))
        val iModLastUnlockApp = iModPerApp.optLong(packageName)

        return iModDelayGlobalEnabled && System.currentTimeMillis() - iModLastUnlockGlobal <= prefsApps.getInt(IMOD_DELAY_GLOBAL, 600000) ||
                iModDelayAppEnabled && System.currentTimeMillis() - iModLastUnlockApp <= prefsApps.getInt(IMOD_DELAY_APP, 600000)
    }

    @JvmStatic
    fun appUnlocked(packageName: String, prefsHistory: SharedPreferences) {
        addToHistory(UNLOCK_ID, Common.MAXLOCK_PACKAGE_NAME, prefsHistory)
        prefsHistory.edit()
                .putLong(IMOD_LAST_UNLOCK_GLOBAL, System.currentTimeMillis())
                .putString(IMOD_APPS, JSONObject(prefsHistory.getString(IMOD_APPS, JSONObject().toString())).put(packageName, System.currentTimeMillis()).toString())
                .apply()
    }

    @JvmStatic
    fun appClosed(packageName: String, prefsHistory: SharedPreferences) {
        prefsHistory.edit()
                .putString(CLOSE_APPS, JSONObject(prefsHistory.getString(CLOSE_APPS, JSONObject().toString())).put(packageName, System.currentTimeMillis()).toString())
                .apply()
    }

    @JvmStatic
    fun wasAppClosed(packageName: String, prefsHistory: SharedPreferences): Boolean {
        val close = JSONObject(prefsHistory.getString(CLOSE_APPS, JSONObject().toString()))
        return System.currentTimeMillis() - close.optLong(packageName) <= 800
    }

    @JvmStatic
    fun addToHistory(taskId: Int, packageName: String, prefsHistory: SharedPreferences) {
        val procs = JSONArray(prefsHistory.getString(PROCESS_HISTORY_ARRAY, JSONArray().toString()))
        val pkgs = JSONArray(prefsHistory.getString(PACKAGE_HISTORY_ARRAY, JSONArray().toString()))
        // Only add task id if new task got launched or if we are in legacy mode anyway
        if (taskId != procs.optInt(0) || taskId == -1) {
            // If new task doesn't have same package name, keep (shift back) the previous task id
            if (packageName != pkgs.optString(0)) {
                procs.put(1, procs.optInt(0))
            }
            procs.put(0, taskId)
        }
        // Shift back package names
        pkgs.put(2, pkgs.optString(1)).put(1, pkgs.optString(0)).put(0, packageName)
        prefsHistory.edit().putString(PROCESS_HISTORY_ARRAY, procs.toString()).putString(PACKAGE_HISTORY_ARRAY, pkgs.toString()).apply()
    }

    @JvmStatic
    fun trim(prefsHistory: SharedPreferences) {
        prefsHistory.edit().remove(PROCESS_HISTORY_ARRAY).remove(PACKAGE_HISTORY_ARRAY).remove(CLOSE_APPS).apply()
    }

    @JvmStatic
    fun getLauncherPackage(packageManager: PackageManager): String =
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addCategory(Intent.CATEGORY_DEFAULT).resolveActivity(packageManager).packageName
}