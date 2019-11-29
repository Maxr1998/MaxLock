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

package de.Maxr1998.xposed.maxlock.daemon

import android.app.IActivityManager
import android.app.IProcessObserver
import android.content.ComponentName
import android.content.Intent
import android.os.*
import android.util.Log
import android_hidden.app.ActivityManager
import android_hidden.app.ActivityOptions
import de.Maxr1998.xposed.maxlock.BuildConfig
import de.Maxr1998.xposed.maxlock.Common.*
import de.Maxr1998.xposed.maxlock.ui.LockActivity
import de.Maxr1998.xposed.maxlock.util.AppLockHelper
import eu.chainfire.librootjavadaemon.RootDaemon

class MaxLockDaemon {
    private var activityManager: IActivityManager = ActivityManager.getService()
    private val resultReceiver: IBinder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                AppLockHelper.UNLOCK_CODE -> {
                    val packageName = data.readString()
                    if (packageName != null)
                        appLockHelper.appUnlocked(packageName)
                    true
                }
                AppLockHelper.CLOSE_CODE -> {
                    // TODO
                    true
                }
                else -> false
            }
        }
    }
    private val processObserver: IProcessObserver = object : IProcessObserver.Stub() {
        override fun onForegroundActivitiesChanged(pid: Int, uid: Int, foregroundActivities: Boolean) {
            try {
                if (foregroundActivities) {
                    val stackInfo = getForegroundStackInfo()
                    val activity = stackInfo.topActivity
                    val taskId = stackInfo.taskIds[0]
                    val packageName = activity.packageName
                    val activityName = activity.className
                    if (appLockHelper.wasAppSwitch(taskId, packageName) &&
                            appLockHelper.isAppLocked(packageName, activityName)) {
                        val intent = Intent(LOCKSCREEN_BASE_INTENT)
                                .putExtra(INTENT_EXTRA_APP_NAMES, arrayOf(packageName, activityName) as Array<String>)
                                .putExtra(INTENT_EXTRA_BINDER_BUNDLE, Bundle().apply {
                                    putBinder(BUNDLE_KEY_BINDER, resultReceiver)
                                })
                        val options = ActivityOptions.makeBasic().apply {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                                launchStackId = stackInfo.stackId
                            launchTaskId = taskId
                        }
                        activityManager.startActivity(null, null, intent, null, null, TAG, 1, 0, null, options.toBundle())
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Exception in ProcessObserver", t)
            }
        }

        override fun onProcessDied(pid: Int, uid: Int) {}
    }
    private val preferencesHelper = RemotePreferencesHelper(activityManager)
    private val appLockHelper = AppLockHelper(preferencesHelper)

    init {
        // Unregister any old binder, register observer
        activityManager.apply {
            unregisterProcessObserver(processObserver)
            registerProcessObserver(processObserver)
        }
    }

    fun tearDown() {
        activityManager.unregisterProcessObserver(processObserver)
    }

    fun getForegroundStackInfo(): ActivityManager.StackInfo = when {
        Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 -> activityManager.focusedStackInfo
        else -> activityManager.getStackInfo(activityManager.focusedStackId)
    }

    companion object {
        private const val TAG = "maxlockd"
        val LOCKSCREEN_BASE_INTENT: Intent = Intent()
                .setComponent(ComponentName(BuildConfig.APPLICATION_ID, LockActivity::class.java.name))
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                Log.i(TAG, "Starting up…")
                RootDaemon.daemonize(BuildConfig.APPLICATION_ID, 0, false, null)

                val daemon = MaxLockDaemon()

                // Prepare & Loop
                Looper.prepare()
                Log.i(TAG, "Started successfully, looping now")
                Looper.loop()
                daemon
            } catch (e: Throwable) {
                Log.e(TAG, "Exception in main loop", e)
                null
            }?.tearDown()
        }
    }
}