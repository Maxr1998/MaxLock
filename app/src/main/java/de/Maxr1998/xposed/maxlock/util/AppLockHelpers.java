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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.no_xposed.AppLockService;
import de.robv.android.xposed.XposedBridge;

public final class AppLockHelpers {


    public static final String PROCESS_HISTORY_ARRAY_KEY = "procs";
    public static final String PACKAGE_HISTORY_ARRAY_KEY = "pkgs";
    public static final int UNLOCK_ID = -0x3A8;
    public static final String IMOD_OBJECT_KEY = "iModPerApp";
    public static final String CLOSE_OBJECT_KEY = "close";
    public static final String IMOD_LAST_UNLOCK_GLOBAL = "IMoDGlobalDelayTimer";
    public static final String IMOD_RESET_ON_SCREEN_OFF = "reset_imod_screen_off";
    @SuppressLint("SdCardPath")
    private static final String HISTORY_PATH = "/data/data/" + Common.MAXLOCK_PACKAGE_NAME + "/files/history.json";
    private static final String IMOD_DELAY_APP = "delay_inputperapp";
    private static final String IMOD_DELAY_GLOBAL = "delay_inputgeneral";

    private AppLockHelpers() {
    }

    public static boolean pass(int taskId, @NonNull String packageName, @Nullable String activityName, @NonNull JSONObject history, @NonNull SharedPreferences prefs) throws Throwable {
        writeFile(addToHistory(taskId, packageName, history));
        // MasterSwitch disabled
        if (!prefs.getBoolean(Common.MASTER_SWITCH_ON, true)) {
            return true;
        }

        // Activity got launched/closed
        if (history.getJSONArray(PROCESS_HISTORY_ARRAY_KEY).optInt(1) == UNLOCK_ID) {
            return true;
        }

        // Activity not locked
        if (activityName != null && !prefs.getBoolean(activityName, true)) {
            return true;
        }

        // I.Mod active
        boolean iModDelayGlobalEnabled = prefs.getBoolean(Common.ENABLE_IMOD_DELAY_GLOBAL, false);
        boolean iModDelayAppEnabled = prefs.getBoolean(Common.ENABLE_IMOD_DELAY_APP, false);
        long iModLastUnlockGlobal = history.optLong(IMOD_LAST_UNLOCK_GLOBAL);
        JSONObject iModPerApp = history.optJSONObject(IMOD_OBJECT_KEY);
        long iModLastUnlockApp = 0;
        if (iModPerApp != null) {
            iModLastUnlockApp = iModPerApp.optLong(packageName);
        }

        return (iModDelayGlobalEnabled && (iModLastUnlockGlobal != 0 &&
                System.currentTimeMillis() - iModLastUnlockGlobal <=
                        prefs.getInt(IMOD_DELAY_GLOBAL, 600000)))
                || iModDelayAppEnabled && (iModLastUnlockApp != 0 &&
                System.currentTimeMillis() - iModLastUnlockApp <=
                        prefs.getInt(IMOD_DELAY_APP, 600000));
    }

    public static boolean close(JSONObject history, String packageName) {
        JSONObject close = history.optJSONObject(CLOSE_OBJECT_KEY);
        return close != null && System.currentTimeMillis() - close.optLong(packageName) <= 800;
    }

    public static JSONObject getDefault() throws JSONException {
        JSONObject history = new JSONObject();
        JSONArray procs = new JSONArray();
        JSONArray pkgs = new JSONArray();
        JSONObject iMod = new JSONObject();
        JSONObject close = new JSONObject();
        history.put(PROCESS_HISTORY_ARRAY_KEY, procs)
                .put(PACKAGE_HISTORY_ARRAY_KEY, pkgs)
                .put(IMOD_OBJECT_KEY, iMod)
                .put(CLOSE_OBJECT_KEY, close);
        return history;
    }

    public static JSONObject readFile() throws JSONException {
        JSONObject history;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(HISTORY_PATH), 50);
            String json = reader.readLine();
            reader.close();
            try {
                history = new JSONObject(json);
            } catch (JSONException | NullPointerException e) {
                return getDefault();
            }
            if (!(history.has(PROCESS_HISTORY_ARRAY_KEY) && history.has(PACKAGE_HISTORY_ARRAY_KEY) && history.has(IMOD_OBJECT_KEY) && history.has(CLOSE_OBJECT_KEY))) {
                return getDefault();
            }
        } catch (IOException e) {
            log("ML: File not found or reading error: " + e.getLocalizedMessage());
            return getDefault();
        }
        return history;
    }

    public static void writeFile(@NonNull JSONObject history) throws JSONException {
        try {
            File JSONFile = new File(HISTORY_PATH);
            if (!JSONFile.exists()) {
                throw new FileNotFoundException("File could not be written, as it doesn't exist.");
            }
            FileWriter bw = new FileWriter(HISTORY_PATH);
            bw.write(history.toString());
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject addToHistory(int taskId, String packageName, JSONObject history) throws JSONException {
        JSONArray procs = history.optJSONArray(PROCESS_HISTORY_ARRAY_KEY);
        JSONArray pkgs = history.optJSONArray(PACKAGE_HISTORY_ARRAY_KEY);
        // Only add task id if new task got launched or if we are in legacy mode anyway
        if (taskId != procs.optInt(0) || taskId == -1) {
            // If new task doesn't have same package name, keep (shift back) the previous task id
            if (!packageName.equals(pkgs.optString(0))) {
                procs.put(1, procs.optInt(0));
            }
            procs.put(0, taskId);
        }
        // Shift back package names
        pkgs.put(2, pkgs.optString(1)).put(1, pkgs.optString(0)).put(0, packageName);

        history.put(PROCESS_HISTORY_ARRAY_KEY, procs).put(PACKAGE_HISTORY_ARRAY_KEY, pkgs);
        return history;
    }

    public static void log(String text) {
        try {
            XposedBridge.log(text);
        } catch (Exception e) {
            Log.d(AppLockService.TAG, text);
        }
    }
}