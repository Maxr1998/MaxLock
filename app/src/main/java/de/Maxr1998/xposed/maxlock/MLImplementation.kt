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
import android.view.accessibility.AccessibilityManager
import com.topjohnwu.superuser.Shell
import de.Maxr1998.xposed.maxlock.daemon.MaxLockDaemon
import eu.chainfire.librootjava.AppProcess
import eu.chainfire.librootjavadaemon.RootDaemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MLImplementation {
    const val DEFAULT = 1
    const val ACCESSIBILITY = 2

    private const val runtimePath = "/data/adb/maxlockd"
    private const val scriptPath = "/data/adb/service.d/maxlockd.sh"

    @JvmStatic
    fun getImplementation(prefs: SharedPreferences): Int {
        if (isDaemonRunning()) {
            prefs.edit().putInt(Common.ML_IMPLEMENTATION, DEFAULT).apply() // Force DEFAULT if the daemon is already running
        }
        return prefs.getInt(Common.ML_IMPLEMENTATION, ACCESSIBILITY)
    }

    suspend fun getImplementationCheckRoot(prefs: SharedPreferences): Int {
        val rooted = withContext(Dispatchers.IO) { Shell.rootAccess() }
        return (if (rooted) DEFAULT else ACCESSIBILITY).also {
            prefs.edit().putInt(Common.ML_IMPLEMENTATION, it).apply()
        }
    }

    /**
     * @return true if launched successfully
     */
    fun launchDaemon(context: Context): Boolean {
        // Create maxlockd launch script
        val script = RootDaemon.getLaunchScript(context, MaxLockDaemon::class.java,
                null, runtimePath, null, "maxlockd")
        val scriptString = script.joinToString("\\n")
        // Install launch script to /data/adb/service.d/
        script.add(0, "${AppProcess.BOX} mkdir -p $runtimePath")
        script.add(1, "${AppProcess.BOX} echo -e '#!/system/bin/sh\\n$scriptString' > $scriptPath")
        script.add(2, "${AppProcess.BOX} chmod a+x $scriptPath")
        // Execute!
        return Shell.su(*script.toTypedArray()).exec().isSuccess
    }

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
        return (getImplementation(prefs) == DEFAULT && isDaemonRunning()) || (getImplementation(prefs) == ACCESSIBILITY && isAccessibilityEnabled(c))
    }

    private fun isDaemonRunning() = false // TODO
}