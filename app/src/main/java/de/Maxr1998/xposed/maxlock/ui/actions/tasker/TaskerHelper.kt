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

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.joaomgcd.common.tasker.IntentServiceParallel
import com.twofortyfouram.locale.api.Intent.*
import de.Maxr1998.xposed.maxlock.BuildConfig
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.ui.actions.ActionsHelper
import de.Maxr1998.xposed.maxlock.ui.actions.BundleScrubber
import de.Maxr1998.xposed.maxlock.ui.actions.tasker.TaskerHelper.EVENT_TYPE_EXTRA_KEY
import de.Maxr1998.xposed.maxlock.ui.actions.tasker.TaskerHelper.EVENT_UNLOCK_ATTEMPT
import de.Maxr1998.xposed.maxlock.ui.actions.tasker.TaskerHelper.EVENT_UNLOCK_FAILED
import de.Maxr1998.xposed.maxlock.ui.actions.tasker.TaskerHelper.EVENT_UNLOCK_SUCCESS
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.NotificationHelper
import de.Maxr1998.xposed.maxlock.util.Util
import net.dinglisch.android.tasker.TaskerPlugin
import org.json.JSONException
import org.json.JSONObject

const val EXTRA_ATTEMPT_SUCCESSFUL = "de.Maxr1998.xposed.maxlock.tasker.event.ATTEMPT_SUCCESSFUL"
const val EXTRA_PACKAGE_NAME = "de.Maxr1998.xposed.maxlock.tasker.event.PACKAGE_NAME"

object TaskerHelper {
    const val EVENT_TYPE_EXTRA_KEY = "de.Maxr1998.xposed.maxlock.extra.EVENT_TYPE"
    const val EVENT_UNLOCK_ATTEMPT = 0
    const val EVENT_UNLOCK_SUCCESS = 1
    const val EVENT_UNLOCK_FAILED = -1

    @JvmStatic
    fun sendQueryRequest(c: Context, attemptSuccessful: Boolean, packageName: String) {
        sendQueryRequest(c, attemptSuccessful, packageName, c.packageName + ".ui.actions.tasker.EventUnlockAttempt")
        sendQueryRequest(c, attemptSuccessful, packageName, c.packageName + ".ui.actions.tasker." + if (attemptSuccessful) "EventUnlockSuccess" else "EventUnlockFailed")
    }
}

fun handleActionIntent(context: Context, intent: Intent) {
    if (!MLPreferences.getPreferences(context).getBoolean(Common.ENABLE_TASKER, false) ||
            intent.action != "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
            || BundleScrubber.scrub(intent)) {
        return
    }
    val extra = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE")
    if (BundleScrubber.scrub(extra) || extra == null) {
        return
    }
    Handler(Looper.getMainLooper()).post({
        ActionsHelper.callAction(extra.getInt(ActionsHelper.ACTION_EXTRA_KEY, -1), context)
    })
}

fun handleQueryIntent(context: Context, intent: Intent, result: Bundle): Int {
    if (BuildConfig.DEBUG)
        Log.d(Util.LOG_TAG_TASKER, "Received Tasker intent")

    val messageId = TaskerPlugin.Event.retrievePassThroughMessageID(intent).toString()
    val data = TaskerPlugin.Event.retrievePassThroughData(intent)
    val prefs = MLPreferences.getPreferences(context)
    val taskerQueries: JSONObject
    try {
        taskerQueries = JSONObject(prefs.getString(Common.TASKER_QUERIES, JSONObject().toString()))
    } catch (e: JSONException) {
        return RESULT_CONDITION_UNSATISFIED
    }

    if (taskerQueries.isNull(messageId) || data == null // Process valid request with data only
            || System.currentTimeMillis() - taskerQueries.optLong(messageId) > 800) { // Don't allow timed out requests
        if (BuildConfig.DEBUG)
            Log.d(Util.LOG_TAG_TASKER, "Timeout or wrong request for event")
        return RESULT_CONDITION_UNSATISFIED
    }
    val bundleExtra = intent.getBundleExtra(EXTRA_BUNDLE)
    val config = bundleExtra.getInt(EVENT_TYPE_EXTRA_KEY)
    val successful = data.getBoolean(EXTRA_ATTEMPT_SUCCESSFUL)
    return if (config == EVENT_UNLOCK_ATTEMPT || config == EVENT_UNLOCK_SUCCESS && successful || config == EVENT_UNLOCK_FAILED && !successful) {
        if (TaskerPlugin.Condition.hostSupportsVariableReturn(intent.extras)) {
            val variables = Bundle()
            variables.putString("%attemptsuccessful", successful.toString())
            variables.putString("%packagename", data.getString(EXTRA_PACKAGE_NAME))
            TaskerPlugin.addVariableBundle(result, variables)
        }
        RESULT_CONDITION_SATISFIED
    } else RESULT_CONDITION_UNSATISFIED
}

@Suppress("NOTHING_TO_INLINE")
inline fun IntentServiceParallel.startInForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationHelper.createNotificationChannels(this)
        val notification = Notification.Builder(this, NotificationHelper.TASKER_CHANNEL).build()
        startForeground(1, notification)
    }
}

private fun sendQueryRequest(c: Context, attemptSuccessful: Boolean, packageName: String, activityClass: String) {
    val intent = Intent(com.twofortyfouram.locale.api.Intent.ACTION_REQUEST_QUERY)
    intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_ACTIVITY_CLASS_NAME, activityClass)
    val prefs = MLPreferences.getPreferences(c)
    try {
        val taskerQueries = JSONObject(prefs.getString(Common.TASKER_QUERIES, JSONObject().toString()))
        taskerQueries.put(TaskerPlugin.Event.addPassThroughMessageID(intent).toString(), System.currentTimeMillis())
        // Cleanup
        if (taskerQueries.length() >= 6) {
            val iterator = taskerQueries.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                if (System.currentTimeMillis() - taskerQueries.optLong(key) > 800) {
                    iterator.remove()
                }
            }
        }
        prefs.edit().putString(Common.TASKER_QUERIES, taskerQueries.toString()).apply()
    } catch (e: JSONException) {
        e.printStackTrace()
    }

    val data = Bundle(2)
    data.putBoolean(EXTRA_ATTEMPT_SUCCESSFUL, attemptSuccessful)
    data.putString(EXTRA_PACKAGE_NAME, packageName)
    TaskerPlugin.Event.addPassThroughData(intent, data)
    c.sendBroadcast(intent)
}