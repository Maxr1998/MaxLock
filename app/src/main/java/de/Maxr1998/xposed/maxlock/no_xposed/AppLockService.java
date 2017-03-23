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

package de.Maxr1998.xposed.maxlock.no_xposed;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.MLImplementation;
import de.Maxr1998.xposed.maxlock.ui.LockActivity;
import de.Maxr1998.xposed.maxlock.util.AppLockHelpers;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;

import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.CLOSE_OBJECT_KEY;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.IMOD_RESET_ON_SCREEN_OFF;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.PACKAGE_HISTORY_ARRAY_KEY;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.PROCESS_HISTORY_ARRAY_KEY;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.addToHistory;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.close;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.getDefault;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.pass;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.readFile;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.writeFile;

public class AppLockService extends AccessibilityService {

    public static final String TAG = "AppLockService";

    private SharedPreferences prefs;
    private SharedPreferences prefsApps;

    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (prefsApps.getBoolean(IMOD_RESET_ON_SCREEN_OFF, false)) {
                    writeFile(getDefault());
                    Log.d(TAG, "Screen turned off, locked apps.");
                } else {
                    writeFile(readFile()
                            .put(PROCESS_HISTORY_ARRAY_KEY, new JSONArray())
                            .put(PACKAGE_HISTORY_ARRAY_KEY, new JSONArray())
                            .put(CLOSE_OBJECT_KEY, new JSONObject()));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error in screenOffReceiver", e);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = MLPreferences.getPreferences(this);
        prefsApps = MLPreferences.getPrefsApps(this);

        registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "Started up");
        try {
            String currentPackage = getRootInActiveWindow().getPackageName().toString();
            if (prefsApps.getBoolean(currentPackage, false)) {
                handlePackage(currentPackage);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in handling startup poll", t);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        // If service is not disabled yet
        if (MLImplementation.getImplementation(prefs) == MLImplementation.DEFAULT) {
            stopSelf();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                disableSelf();
            }
            return;
        }

        String packageName = String.valueOf(accessibilityEvent.getPackageName());
        if (packageName.equals("null") ||
                accessibilityEvent.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                packageName.equals("android") || packageName.matches("com\\.(google\\.)?android\\.systemui") ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getWindowType(accessibilityEvent) == AccessibilityWindowInfo.TYPE_SYSTEM) ||
                Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD).startsWith(packageName)) {
            return;
        }

        Log.d(TAG, "Window state changed: " + packageName);
        try {
            if (prefsApps.getBoolean(packageName, false)) {
                handlePackage(packageName);
            } else {
                writeFile(addToHistory(-1, packageName, readFile()));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in handling event", t);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private int getWindowType(AccessibilityEvent accessibilityEvent) {
        for (AccessibilityWindowInfo info : getWindows()) {
            if (accessibilityEvent.getWindowId() == info.getId()) {
                return info.getType();
            }
        }
        return AccessibilityWindowInfo.TYPE_APPLICATION;
    }

    private void handlePackage(String packageName) throws Throwable {
        JSONObject history = AppLockHelpers.readFile();

        if (close(history, packageName)) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            return;
        }

        if (pass(-1, packageName, null, history, prefsApps)) {
            return;
        }

        Log.d(TAG, "Show lockscreen: " + packageName);
        Intent i = new Intent(this, LockActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(Common.INTENT_EXTRAS_NAMES, new String[]{packageName, null});
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(screenOffReceiver);
        performGlobalAction(GLOBAL_ACTION_HOME);
        super.onDestroy();
    }
}