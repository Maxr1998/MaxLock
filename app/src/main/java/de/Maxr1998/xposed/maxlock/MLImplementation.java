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

package de.Maxr1998.xposed.maxlock;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;
import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_VISUAL;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

public final class MLImplementation {

    public static final int DEFAULT = 1;
    public static final int NO_XPOSED = 2;

    private MLImplementation() {
    }

    public static int getImplementation(@NonNull SharedPreferences prefs) {
        if (isXposedActive() || !isAccessibilitySupported()) {
            prefs.edit().putInt(Common.ML_IMPLEMENTATION, DEFAULT).apply(); // Force DEFAULT if below N or Xposed is installed and module is activated
        }
        return prefs.getInt(Common.ML_IMPLEMENTATION, isXposedInstalled() ? DEFAULT : NO_XPOSED); // Return NO_XPOSED as default only if Xposed isn't even installed
    }

    public static boolean isXposedInstalled() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = stack.length - 3; i < stack.length; i++) {
            if (stack[i].toString().contains("de.robv.android.xposed.XposedBridge"))
                return true;
        }
        return false;
    }

    public static boolean isAccessibilitySupported() {
        return SDK_INT >= N;
    }

    @Keep
    public static boolean isXposedActive() {
        return false;
    }

    private static boolean isAccessibilityEnabled(Context c) {
        AccessibilityManager manager = (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> infos = manager.getEnabledAccessibilityServiceList(FEEDBACK_GENERIC | FEEDBACK_VISUAL);
        for (int i = 0; i < infos.size(); i++) {
            String packageName = infos.get(i).getResolveInfo().serviceInfo.packageName; // Ugh. Isn't there sth better? Definitely a // TODO
            if (packageName.equals(BuildConfig.APPLICATION_ID)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isActiveAndWorking(Context c, SharedPreferences prefs) {
        return (getImplementation(prefs) == DEFAULT && isXposedActive()) || (getImplementation(prefs) == NO_XPOSED && isAccessibilityEnabled(c));
    }
}