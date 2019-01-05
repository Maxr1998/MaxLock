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

package de.Maxr1998.xposed.maxlock

import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_VISUAL
import android.content.Context
import android.content.SharedPreferences
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import android.view.accessibility.AccessibilityManager
import androidx.annotation.Keep
import com.topjohnwu.superuser.Shell
import de.Maxr1998.xposed.maxlock.daemon.MaxLockDaemon
import eu.chainfire.librootjavadaemon.RootDaemon

object MLImplementation {
    const val DEFAULT = 1
    const val NO_XPOSED = 2

    @JvmStatic
    fun getImplementation(prefs: SharedPreferences): Int {
        if (isXposedActive() || !isAccessibilitySupported) {
            prefs.edit().putInt(Common.ML_IMPLEMENTATION, DEFAULT).apply() // Force DEFAULT if below N or Xposed is installed and module is activated
        }
        return prefs.getInt(Common.ML_IMPLEMENTATION, if (isXposedInstalled()) DEFAULT else NO_XPOSED) // Return NO_XPOSED as default only if Xposed isn't even installed
    }

    @Keep
    @JvmStatic
    fun isXposedActive(): Boolean = false

    private fun isXposedInstalled(): Boolean {
        val stack = Thread.currentThread().stackTrace
        for (i in stack.size - 3 until stack.size) {
            if (stack[i].toString().contains("de.robv.android.xposed.XposedBridge"))
                return true
        }
        return false
    }

    fun launchDaemon(context: Context) {
        val script = RootDaemon.getLaunchScript(context, MaxLockDaemon::class.java,
                null, null, null, "maxlockd")
        Shell.su(*script.toTypedArray()).exec()
    }

    @JvmStatic
    val isAccessibilitySupported: Boolean
        get() = SDK_INT >= N

    private fun isAccessibilityEnabled(c: Context): Boolean {
        val manager = c.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val infos = manager.getEnabledAccessibilityServiceList(FEEDBACK_GENERIC or FEEDBACK_VISUAL)
        for (i in infos.indices) {
            // Ugh. Isn't there sth better? Definitely a // TODO
            if (infos[i].resolveInfo.serviceInfo.packageName == BuildConfig.APPLICATION_ID)
                return true
        }
        return false
    }

    @JvmStatic
    fun isActiveAndWorking(c: Context, prefs: SharedPreferences): Boolean {
        return getImplementation(prefs) == DEFAULT && isXposedActive() || getImplementation(prefs) == NO_XPOSED && isAccessibilityEnabled(c)
    }
}