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

package de.Maxr1998.xposed.maxlock.hooks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Apps {

    @SuppressLint("SdCardPath")
    public static final String HISTORY_PATH = "/data/data/" + Main.MAXLOCK_PACKAGE_NAME + "/files/history.json";
    public static final String HISTORY_ARRAY_KEY = "history";
    public static final int UNLOCK_ID = -0x3A8;
    public static final String IMOD_OBJECT_KEY = "iModPerApp";
    public static final String FLAG_IMOD = "_imod";
    public static final String CLOSE_OBJECT_KEY = "close";
    public static final String FLAG_CLOSE_APP = "_close";

    public static final String IMOD_DELAY_APP = "delay_inputperapp";
    public static final String IMOD_DELAY_GLOBAL = "delay_inputgeneral";
    public static final String IMOD_LAST_UNLOCK_GLOBAL = "IMoDGlobalDelayTimer";

    public static void initLogging(final XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod("android.app.Activity", lPParam.classLoader, "onStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    writeFile(addToHistory(((Activity) param.thisObject).getTaskId(), lPParam.packageName, readFile()));
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void init(final XSharedPreferences prefsApps, final XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod("android.app.Activity", lPParam.classLoader, "onStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Activity app = (Activity) param.thisObject;
                    String activityName = app.getClass().getName();
                    log("MLaS|" + activityName + "||" + System.currentTimeMillis());
                    JSONObject history = readFile();
                    JSONObject close = history.optJSONObject(CLOSE_OBJECT_KEY);
                    if (close != null && System.currentTimeMillis() - close.optLong(lPParam.packageName + FLAG_CLOSE_APP) <= 800) {
                        app.finish();
                        return;
                    }
                    prefsApps.reload();
                    if (activityName.equals("android.app.Activity") ||
                            pass(app.getTaskId(), lPParam.packageName, activityName, history, prefsApps)) {
                        return;
                    }
                    Intent it = new Intent();
                    it.setComponent(new ComponentName(Main.MAXLOCK_PACKAGE_NAME, Main.MAXLOCK_PACKAGE_NAME + ".ui.LockActivity"));
                    it.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    if (prefsApps.getBoolean(lPParam.packageName + "_fake", false)) {
                        it.putExtra(Common.LOCK_ACTIVITY_MODE, Common.MODE_FAKE_CRASH);
                    }
                    it.putExtra(Common.INTENT_EXTRAS_NAMES, new String[]{lPParam.packageName, activityName});
                    app.startActivity(it);
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }

    private static boolean pass(int taskId, @NonNull String packageName, String activityName, @NonNull JSONObject history, @NonNull XSharedPreferences prefs) throws Throwable {
        writeFile(addToHistory(taskId, packageName, history));
        List<Integer> historyList = new ArrayList<>();
        String[] list = history.getJSONArray(HISTORY_ARRAY_KEY).toString().replaceAll("[\\[\\]]", "").split(",");
        for (int i = 0; i < 2; i++) {
            historyList.add(Integer.parseInt(list[i]));
        }
        // MasterSwitch disabled
        if (!prefs.getBoolean(Common.MASTER_SWITCH_ON, true)) {
            return true;
        }
        // Activity got launched/closed
        if (historyList.indexOf(UNLOCK_ID) == 1) {
            return true;
        }

        // Activity not locked
        if (!prefs.getBoolean(activityName, true)) {
            return true;
        }

        // I.Mod active
        boolean iModDelayGlobalEnabled = prefs.getBoolean(Common.ENABLE_IMOD_DELAY_GLOBAL, false);
        boolean iModDelayAppEnabled = prefs.getBoolean(Common.ENABLE_IMOD_DELAY_APP, false);
        long iModLastUnlockGlobal = history.optLong(IMOD_LAST_UNLOCK_GLOBAL);
        JSONObject iModPerApp = history.optJSONObject(IMOD_OBJECT_KEY);
        long iModLastUnlockApp = 0;
        if (iModPerApp != null) {
            iModLastUnlockApp = iModPerApp.optLong(packageName + FLAG_IMOD);
        }

        return (iModDelayGlobalEnabled && (iModLastUnlockGlobal != 0 &&
                System.currentTimeMillis() - iModLastUnlockGlobal <=
                        prefs.getInt(IMOD_DELAY_GLOBAL, 600000)))
                || iModDelayAppEnabled && (iModLastUnlockApp != 0 &&
                System.currentTimeMillis() - iModLastUnlockApp <=
                        prefs.getInt(IMOD_DELAY_APP, 600000));
    }

    public static JSONObject getDefault() throws Throwable {
        JSONObject history = new JSONObject();
        JSONArray array = new JSONArray();
        JSONObject iMod = new JSONObject();
        JSONObject close = new JSONObject();
        history.put(HISTORY_ARRAY_KEY, array);
        history.put(IMOD_OBJECT_KEY, iMod);
        history.put(CLOSE_OBJECT_KEY, close);
        return history;
    }

    public static JSONObject readFile() throws Throwable {
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
            if (!(history.has(HISTORY_ARRAY_KEY) && history.has(IMOD_OBJECT_KEY) && history.has(CLOSE_OBJECT_KEY))) {
                return getDefault();
            }
        } catch (FileNotFoundException e) {
            log("ML: File not found: " + e.getLocalizedMessage());
            return getDefault();
        }
        return history;
    }

    public static void writeFile(@NonNull JSONObject history) throws Throwable {
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

    public static JSONObject addToHistory(int taskId, String packageName, JSONObject history) throws Throwable {
        JSONArray array = history.optJSONArray(HISTORY_ARRAY_KEY);
        array.put(4, array.optString(3));
        array.put(3, array.optString(2));
        array.put(2, packageName);
        if (array.optInt(0) != taskId) {
            array.put(1, array.optInt(0));
            array.put(0, taskId);
        }
        history.put(HISTORY_ARRAY_KEY, array);
        return history;
    }
}