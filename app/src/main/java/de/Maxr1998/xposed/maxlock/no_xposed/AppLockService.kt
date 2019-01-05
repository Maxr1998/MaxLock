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

package de.Maxr1998.xposed.maxlock.no_xposed

import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.Common.RESET_RELOCK_TIMER_ON_HOMESCREEN
import de.Maxr1998.xposed.maxlock.Common.RESET_RELOCK_TIMER_ON_SCREEN_OFF
import de.Maxr1998.xposed.maxlock.MLImplementation
import de.Maxr1998.xposed.maxlock.ui.LockActivity
import de.Maxr1998.xposed.maxlock.util.AppLockHelpers.addToHistory
import de.Maxr1998.xposed.maxlock.util.AppLockHelpers.getLauncherPackage
import de.Maxr1998.xposed.maxlock.util.AppLockHelpers.pass
import de.Maxr1998.xposed.maxlock.util.AppLockHelpers.trim
import de.Maxr1998.xposed.maxlock.util.MLPreferences

@TargetApi(Build.VERSION_CODES.N)
class AppLockService : AccessibilityService() {

    private val TAG = "AppLockService"

    private val prefs by lazy { MLPreferences.getPreferences(this) }
    private val prefsApps by lazy { MLPreferences.getPrefsApps(this) }
    private val prefsHistory by lazy { MLPreferences.getPrefsHistory(this) }

    private val launcherPackage by lazy { getLauncherPackage(packageManager) }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (prefsApps.getBoolean(RESET_RELOCK_TIMER_ON_SCREEN_OFF, false)) {
                prefsHistory.edit().clear().apply()
                Log.d(TAG, "Screen turned off, locked apps.")
            } else {
                trim(prefsHistory)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onServiceConnected() {
        Log.i(TAG, "Started up")
        try {
            rootInActiveWindow?.packageName?.toString()?.let {
                if (prefsApps.getBoolean(it, false)) {
                    handlePackage(it)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in handling startup poll", t)
        }
    }

    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
        // If service is not disabled yet
        if (MLImplementation.getImplementation(prefs) == MLImplementation.DEFAULT) {
            stopSelf()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                disableSelf()
            }
            return
        }

        val packageName = accessibilityEvent.packageName?.toString().orEmpty()
        if (ignoreEvent(accessibilityEvent, packageName)) {
            return
        }

        Log.d(TAG, "Window state changed: $packageName")
        try {
            if (prefsApps.getBoolean(packageName, false)) {
                handlePackage(packageName)
            } else {
                addToHistory(-1, packageName, prefsHistory)
                if (packageName == launcherPackage && prefsApps.getBoolean(RESET_RELOCK_TIMER_ON_HOMESCREEN, false)) {
                    prefsHistory.edit().clear().apply()
                    Log.d(TAG, "Returned to homescreen, locked apps")
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in handling event", t)
        }
    }

    private fun ignoreEvent(event: AccessibilityEvent, packageName: String): Boolean {
        return packageName.isEmpty() ||
                event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !isApplication(event) ||
                packageName == "android" || packageName.matches("com\\.(google\\.)?android\\.systemui".toRegex()) ||
                Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).startsWith(packageName)
    }

    private fun isApplication(accessibilityEvent: AccessibilityEvent): Boolean {
        windows
                .filter { it.id == accessibilityEvent.windowId }
                .firstOrNull { return it == null || it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        return true
    }

    @Throws(Throwable::class)
    private fun handlePackage(packageName: String) {
        if (pass(-1, packageName, null, prefsApps, prefsHistory)) {
            return
        }

        Log.d(TAG, "Show lockscreen: $packageName")
        val i = Intent(this, LockActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_FROM_BACKGROUND or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra(Common.INTENT_EXTRAS_NAMES, arrayOf(packageName, ""))
        if (prefsApps.getBoolean(packageName + "_fake", false)) {
            i.putExtra(Common.LOCK_ACTIVITY_MODE, Common.MODE_FAKE_CRASH)
        }
        startActivity(i)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        if (MLImplementation.getImplementation(prefs) != MLImplementation.DEFAULT && MLImplementation.isActiveAndWorking(this, prefs))
            performGlobalAction(GLOBAL_ACTION_HOME)
        super.onDestroy()
    }
}