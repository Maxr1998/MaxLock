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
import de.Maxr1998.xposed.maxlock.Common.*
import de.Maxr1998.xposed.maxlock.daemon.RemotePreferencesHelper

class AppLockHelper private constructor(val prefsApps: SharedPreferences?, val helper: RemotePreferencesHelper?) {
    constructor(prefsApps: SharedPreferences) : this(prefsApps, null)
    constructor(helper: RemotePreferencesHelper) : this(null, helper)

    private val taskStack = IntArray(2) { -1 }
    private val pkgStack = Array(3) { "" }

    private var lastUnlockGlobal = 0L
    private var lastUnlockPerApp = HashMap<String, Long>()

    fun isAppLocked(packageName: String, activityName: String?): Boolean {
        // App was just unlocked
        if (taskStack[1] == UNLOCK_ID)
            return false

        // App not locked or master switch off
        if (!getBooleanSetting(packageName, false) || !getBooleanSetting(MASTER_SWITCH_ON, true))
            return false

        // Activity not locked/excluded
        if (activityName != null && !getBooleanSetting(activityName, true))
            return false

        // Check delayed relock
        return !isTemporarilyUnlocked(packageName)
    }

    fun isTemporarilyUnlocked(packageName: String): Boolean {
        // General
        if (getBooleanSetting(ENABLE_DELAY_GENERAL, false)) {
            val delay = getIntSetting(DELAY_GENERAL, 600000)
            if (System.currentTimeMillis() - lastUnlockGlobal <= delay)
                return true
        }
        // Per app
        if (getBooleanSetting(ENABLE_DELAY_PER_APP, false)) {
            val delay = getIntSetting(DELAY_PER_APP, 600000)
            if (System.currentTimeMillis() - (lastUnlockPerApp[packageName] ?: 0) <= delay)
                return true
        }
        return false
    }

    fun appUnlocked(packageName: String) {
        taskStack[1] = taskStack[0]
        taskStack[0] = UNLOCK_ID
        System.arraycopy(pkgStack, 0, pkgStack, 1, 2)
        pkgStack[0] = Common.MAXLOCK_PACKAGE_NAME

        lastUnlockGlobal = System.currentTimeMillis()
        lastUnlockPerApp[packageName] = System.currentTimeMillis()
    }

    fun wasAppSwitch(taskId: Int, packageName: String): Boolean {
        // Suppress for recents
        if (packageName == "com.android.systemui" || packageName == "com.android.launcher3")
            return false

        var appSwitch = false
        // Only add task id if new task got launched or if we are in legacy mode anyway
        if (taskId != taskStack[0] || taskId == -1) {
            // If new task doesn't have same package name, shift back the previous task id,
            // essentially "forgetting" the unlock for the current app
            if (packageName != pkgStack[0]) {
                taskStack[1] = taskStack[0]
                appSwitch = true
            }
            taskStack[0] = taskId
        }
        // Shift back package names
        System.arraycopy(pkgStack, 0, pkgStack, 1, 2)
        pkgStack[0] = packageName
        return appSwitch
    }

    private fun getIntSetting(key: String, default: Int) = when {
        prefsApps != null -> prefsApps.getInt(key, default)
        helper != null -> helper.queryInt(PREFS_APPS, key, default)
        else -> default
    }

    private fun getBooleanSetting(key: String, default: Boolean) = when {
        prefsApps != null -> prefsApps.getBoolean(key, default)
        helper != null -> helper.queryInt(PREFS_APPS, key, if (default) 1 else 0) != 0
        else -> default
    }

    fun relock() {
        taskStack.fill(-1)
        pkgStack.fill("")
    }

    fun resetTimers() {
        relock()
        lastUnlockGlobal = 0
        lastUnlockPerApp.clear()
    }

    companion object {
        const val UNLOCK_ID = -6295625
        const val UNLOCK_CODE = -UNLOCK_ID
        const val CLOSE_CODE = 25673

        @JvmStatic
        fun getLauncherPackage(packageManager: PackageManager): String =
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addCategory(Intent.CATEGORY_DEFAULT).resolveActivity(packageManager).packageName
    }
}