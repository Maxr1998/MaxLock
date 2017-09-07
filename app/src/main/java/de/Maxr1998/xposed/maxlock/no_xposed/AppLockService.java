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

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.MLImplementation;
import de.Maxr1998.xposed.maxlock.ui.LockActivity;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import hugo.weaving.DebugLog;

import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.IMOD_RESET_ON_SCREEN_OFF;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.addToHistory;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.pass;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.trim;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.wasAppClosed;

@TargetApi(Build.VERSION_CODES.N)
public class AppLockService extends AccessibilityService {

    public static final String TAG = "AppLockService";

    private SharedPreferences prefs;
    private SharedPreferences prefsApps;
    private SharedPreferences prefsHistory;

    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (prefsApps.getBoolean(IMOD_RESET_ON_SCREEN_OFF, false)) {
                prefsHistory.edit().clear().apply();
                Log.d(TAG, "Screen turned off, locked apps.");
            } else {
                trim(prefsHistory);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = MLPreferences.getPreferences(this);
        prefsApps = MLPreferences.getPrefsApps(this);
        prefsHistory = MLPreferences.getPrefsHistory(this);

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

        final String packageName = String.valueOf(accessibilityEvent.getPackageName());
        if (ignoreEvent(accessibilityEvent, packageName)) {
            return;
        }

        Log.d(TAG, "Window state changed: " + packageName);
        try {
            if (prefsApps.getBoolean(packageName, false)) {
                handlePackage(packageName);
            } else {
                addToHistory(-1, packageName, prefsHistory);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in handling event", t);
        }
    }

    @DebugLog
    private boolean ignoreEvent(AccessibilityEvent event, String packageName) {
        return packageName.equals("null") ||
                event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.getSource().getWindow() == null || !isApplication(event) ||
                packageName.equals("android") || packageName.matches("com\\.(google\\.)?android\\.systemui") ||
                Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD).startsWith(packageName);
    }

    @DebugLog
    private boolean isApplication(AccessibilityEvent accessibilityEvent) {
        for (AccessibilityWindowInfo info : getWindows()) {
            if (accessibilityEvent.getWindowId() == info.getId()) {
                return info.getType() == AccessibilityWindowInfo.TYPE_APPLICATION;
            }
        }
        return true;
    }

    private void handlePackage(String packageName) throws Throwable {
        if (wasAppClosed(packageName, prefsHistory)) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            return;
        }

        if (pass(-1, packageName, null, prefsApps, prefsHistory)) {
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