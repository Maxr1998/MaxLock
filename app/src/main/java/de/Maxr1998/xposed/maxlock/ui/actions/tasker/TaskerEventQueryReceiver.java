/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2016 Max Rumpf alias Maxr1998
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

package de.Maxr1998.xposed.maxlock.ui.actions.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.lib.TaskerPlugin;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static com.twofortyfouram.locale.api.Intent.ACTION_REQUEST_QUERY;
import static com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE;
import static com.twofortyfouram.locale.api.Intent.EXTRA_STRING_ACTIVITY_CLASS_NAME;
import static com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED;
import static com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED;


public class TaskerEventQueryReceiver extends BroadcastReceiver {

    public static final String EVENT_TYPE_EXTRA_KEY = "de.Maxr1998.xposed.maxlock.extra.EVENT_TYPE";
    public static final int EVENT_UNLOCK_ATTEMPT = 0;
    public static final int EVENT_UNLOCK_SUCCESS = 1;
    public static final int EVENT_UNLOCK_FAILED = -1;

    private static final String EXTRA_ATTEMPT_SUCCESSFUL = "de.Maxr1998.xposed.maxlock.tasker.event.ATTEMPT_SUCCESSFUL";
    private static final String EXTRA_PACKAGE_NAME = "de.Maxr1998.xposed.maxlock.tasker.event.PACKAGE_NAME";

    public static void sendRequest(Context c, boolean attemptSuccessful, String packageName) {
        send(c, attemptSuccessful, packageName, c.getPackageName() + ".ui.actions.tasker.EventUnlockAttempt");
        send(c, attemptSuccessful, packageName, c.getPackageName() + ".ui.actions.tasker." + (attemptSuccessful ? "EventUnlockSuccess" : "EventUnlockFailed"));
    }

    private static void send(Context c, boolean attemptSuccessful, String packageName, String activityClass) {
        Intent intent = new Intent(ACTION_REQUEST_QUERY);
        intent.putExtra(EXTRA_STRING_ACTIVITY_CLASS_NAME, activityClass);
        SharedPreferences prefs = MLPreferences.getPreferences(c);
        try {
            JSONObject taskerQueries = new JSONObject(prefs.getString(Common.TASKER_QUERIES, new JSONObject().toString()));
            taskerQueries.put(String.valueOf(TaskerPlugin.Event.addPassThroughMessageID(intent)), System.currentTimeMillis());
            // Cleanup
            if (taskerQueries.length() >= 6) {
                Iterator<String> iterator = taskerQueries.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (System.currentTimeMillis() - taskerQueries.optLong(key) > 800) {
                        iterator.remove();
                    }
                }
            }
            prefs.edit().putString(Common.TASKER_QUERIES, taskerQueries.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Bundle data = new Bundle(2);
        data.putBoolean(EXTRA_ATTEMPT_SUCCESSFUL, attemptSuccessful);
        data.putString(EXTRA_PACKAGE_NAME, packageName);
        TaskerPlugin.Event.addPassThroughData(intent, data);
        c.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG)
            Log.d(Util.LOG_TAG_TASKER, "Received Tasker intent");

        String messageId = String.valueOf(TaskerPlugin.Event.retrievePassThroughMessageID(intent));
        Bundle data = TaskerPlugin.Event.retrievePassThroughData(intent);
        SharedPreferences prefs = MLPreferences.getPreferences(context);
        JSONObject taskerQueries;
        try {
            taskerQueries = new JSONObject(prefs.getString(Common.TASKER_QUERIES, new JSONObject().toString()));
        } catch (JSONException e) {
            setResultCode(RESULT_CONDITION_UNSATISFIED);
            return;
        }
        if (taskerQueries.isNull(messageId) || data == null // Process valid request with data only
                || System.currentTimeMillis() - taskerQueries.optLong(messageId) > 800) { // Don't allow timed out requests
            if (BuildConfig.DEBUG)
                Log.d(Util.LOG_TAG_TASKER, "Timeout or wrong request for event");
            setResultCode(RESULT_CONDITION_UNSATISFIED);
            return;
        }
        Bundle bundleExtra = intent.getBundleExtra(EXTRA_BUNDLE);
        int config = bundleExtra.getInt(EVENT_TYPE_EXTRA_KEY);
        boolean successful = data.getBoolean(EXTRA_ATTEMPT_SUCCESSFUL);
        if (config == EVENT_UNLOCK_ATTEMPT || (config == EVENT_UNLOCK_SUCCESS && successful) || (config == EVENT_UNLOCK_FAILED && !successful)) {
            Bundle result = new Bundle();
            if (TaskerPlugin.Condition.hostSupportsVariableReturn(intent.getExtras())) {
                Bundle variables = new Bundle();
                variables.putString("%attemptsuccessful", String.valueOf(successful));
                variables.putString("%packagename", data.getString(EXTRA_PACKAGE_NAME));
                TaskerPlugin.addVariableBundle(result, variables);
            }
            setResult(RESULT_CONDITION_SATISFIED, "", result);
        } else setResultCode(RESULT_CONDITION_UNSATISFIED);
    }
}