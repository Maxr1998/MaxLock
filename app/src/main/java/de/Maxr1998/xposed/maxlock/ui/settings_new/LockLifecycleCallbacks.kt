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

package de.Maxr1998.xposed.maxlock.ui.settings_new

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler

class LockLifecycleCallbacks(private val vm: SettingsViewModel) : Application.ActivityLifecycleCallbacks {
    private val handler = Handler()
    private val lock = Runnable { vm.locked = true }

    override fun onActivityStarted(activity: Activity) {
        handler.postDelayed({
            // Delayed to make sure the previous activity
            // was stopped already
            handler.removeCallbacks(lock)
        }, 3000)
    }

    override fun onActivityStopped(activity: Activity) {
        // Lock in 5 seconds if no other activity removes our callback
        handler.postDelayed(lock, 5000)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}
}