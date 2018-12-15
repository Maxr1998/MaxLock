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

package de.Maxr1998.xposed.maxlock.ui.actions.tasker

import android.app.Activity
import android.content.Intent
import com.joaomgcd.common.tasker.IntentServiceParallel
import net.dinglisch.android.tasker.TaskerPlugin

class TaskerActionService : IntentServiceParallel("ML-TaskerActionService") {
    override fun onHandleIntent(intent: Intent) {
        startInForeground()
        handleActionIntent(this, intent)
        TaskerPlugin.Setting.signalFinish(this, intent, Activity.RESULT_OK, null)
    }
}