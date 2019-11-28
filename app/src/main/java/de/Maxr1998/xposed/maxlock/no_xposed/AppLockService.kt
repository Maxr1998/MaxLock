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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.Common.RESET_RELOCK_TIMER_ON_HOMESCREEN
import de.Maxr1998.xposed.maxlock.Common.RESET_RELOCK_TIMER_ON_SCREEN_OFF
import de.Maxr1998.xposed.maxlock.MLImplementation
import de.Maxr1998.xposed.maxlock.ui.LockActivity
import de.Maxr1998.xposed.maxlock.util.AppLockHelper
import de.Maxr1998.xposed.maxlock.util.AppLockHelper.Companion.getLauncherPackage
import de.Maxr1998.xposed.maxlock.util.MLPreferences

class AppLockService : AccessibilityService() {

    private val TAG = "AppLockService"

    private val appLockHelper by lazy { AppLockHelper(prefsApps) }

    private val prefs by lazy { MLPreferences.getPreferences(this) }
    private val prefsApps by lazy { MLPreferences.getPrefsApps(this) }

    private val launcherPackage by lazy { getLauncherPackage(packageManager) }

    private val resultReceiver: IBinder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                AppLockHelper.UNLOCK_CODE -> {
                    val packageName = data.readString()
                    if (packageName != null)
                        appLockHelper.appUnlocked(packageName)
                    true
                }
                else -> false
            }
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (prefsApps.getBoolean(RESET_RELOCK_TIMER_ON_SCREEN_OFF, false)) {
                appLockHelper.resetTimers()
                Log.d(TAG, "Screen turned off, locked apps.")
            } else appLockHelper.relock()
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
                if (appLockHelper.isAppLocked(it, null)) {
                    lockApp(it)
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
            disableSelf()
            return
        }

        val packageName = accessibilityEvent.packageName?.toString().orEmpty()
        if (ignoreEvent(accessibilityEvent, packageName)) {
            return
        }

        Log.d(TAG, "Window state changed: $packageName")
        try {
            if (appLockHelper.wasAppSwitch(-1, packageName) &&
                    appLockHelper.isAppLocked(packageName, null)) {
                lockApp(packageName)
            } else if (packageName == launcherPackage && prefsApps.getBoolean(RESET_RELOCK_TIMER_ON_HOMESCREEN, false)) {
                appLockHelper.resetTimers()
                Log.d(TAG, "Returned to homescreen, locked apps")
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
    private fun lockApp(packageName: String) {
        Log.d(TAG, "Show lockscreen: $packageName")
        val i = Intent(this, LockActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_FROM_BACKGROUND or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra(Common.INTENT_EXTRA_APP_NAMES, arrayOf(packageName, ""))
                .putExtra(Common.INTENT_EXTRA_BINDER_BUNDLE, Bundle().apply {
                    putBinder(Common.BUNDLE_KEY_BINDER, resultReceiver)
                })
        if (prefsApps.getBoolean(packageName + "_fake", false)) {
            i.putExtra(Common.INTENT_EXTRA_LOCK_ACTIVITY_MODE, Common.MODE_FAKE_CRASH)
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