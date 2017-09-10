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

package de.Maxr1998.xposed.maxlock.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import de.Maxr1998.xposed.maxlock.Common;

public class MLPreferences {

    private static WeakReference<SharedPreferences> PREFS = new WeakReference<>(null);
    private static WeakReference<SharedPreferences> PREFS_APPS = new WeakReference<>(null);
    private static WeakReference<SharedPreferences> PREFS_HISTORY = new WeakReference<>(null);
    private static WeakReference<SharedPreferences> PREFS_KEY = new WeakReference<>(null);
    private static WeakReference<SharedPreferences> PREFS_KEYS_PER_APP = new WeakReference<>(null);

    @NonNull
    public static SharedPreferences getPreferences(Context context) {
        if (PREFS.get() == null) {
            PREFS = new WeakReference<>(PreferenceManager.getDefaultSharedPreferences(context));
        }
        return PREFS.get();
    }

    @NonNull
    public static SharedPreferences getPrefsApps(Context context) {
        if (PREFS_APPS.get() == null) {
            PREFS_APPS = new WeakReference<>(context.getSharedPreferences(Common.PREFS_APPS, Context.MODE_PRIVATE));
        }
        return PREFS_APPS.get();
    }

    @NonNull
    public static SharedPreferences getPrefsHistory(Context context) {
        if (PREFS_HISTORY.get() == null) {
            PREFS_HISTORY = new WeakReference<>(context.getSharedPreferences(Common.PREFS_HISTORY, Context.MODE_PRIVATE));
        }
        return PREFS_HISTORY.get();
    }

    @NonNull
    public static SharedPreferences getPreferencesKeys(Context context) {
        if (PREFS_KEY.get() == null) {
            PREFS_KEY = new WeakReference<>(context.getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE));
        }
        return PREFS_KEY.get();
    }

    @NonNull
    public static SharedPreferences getPreferencesKeysPerApp(Context context) {
        if (PREFS_KEYS_PER_APP.get() == null) {
            PREFS_KEYS_PER_APP = new WeakReference<>(context.getSharedPreferences(Common.PREFS_KEYS_PER_APP, Context.MODE_PRIVATE));
        }
        return PREFS_KEYS_PER_APP.get();
    }
}