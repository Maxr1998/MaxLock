/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2015  Maxr1998
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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Apps {

    public static final String FLAG_CLOSE_APP = "_close";
    public static final String FLAG_TMP = "_tmp";
    public static final String FLAG_IMOD = "_imod";
    public static final String IMOD_DELAY_APP = "delay_inputperapp";
    public static final String IMOD_DELAY_GLOBAL = "delay_inputgeneral";
    public static final String IMOD_LAST_UNLOCK_GLOBAL = "IMoDGlobalDelayTimer";
    @SuppressLint("SdCardPath")
    public static final String HISTORY_PATH = "/data/data/" + Main.MAXLOCK_PACKAGE_NAME + "/files/history.json";
    public static final String HISTORY_ARRAY_KEY = "history";

    public static void initLogging(final XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod("android.app.Activity", lPParam.classLoader, "onStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    addToHistory(lPParam.packageName);
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
                    Activity app = (Activity) param.thisObject;
                    log("MLaS|" + app.getClass().getName() + "||" + System.currentTimeMillis());
                    if (System.currentTimeMillis() - get(lPParam.packageName + FLAG_CLOSE_APP) <= 800) {
                        app.finish();
                        return;
                    }
                    prefsApps.reload();
                    if (app.getClass().getName().equals("android.app.Activity") ||
                            pass(lPParam.packageName, app.getClass().getName(), prefsApps)) {
                        return;
                    }
                    Intent it = new Intent();
                    it.setComponent(new ComponentName(Main.MAXLOCK_PACKAGE_NAME, Main.MAXLOCK_PACKAGE_NAME + ".ui.LockActivity"));
                    it.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    it.putExtra(Common.LOCK_ACTIVITY_MODE, prefsApps.getBoolean(lPParam.packageName + "_fake", false) ? Common.MODE_FAKE_DIE : Common.MODE_DEFAULT);
                    it.putExtra(Common.INTENT_EXTRAS_INTENT, app.getIntent());
                    it.putExtra(Common.INTENT_EXTRAS_PKG_NAME, lPParam.packageName);
                    app.startActivity(it);
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }

    private static boolean pass(String packageName, String activityName, @NonNull XSharedPreferences prefs) throws Throwable {
        addToHistory(packageName);
        // MasterSwitch disabled
        if (!prefs.getBoolean(Common.MASTER_SWITCH_ON, true)) {
            return true;
        }
        // Activity not locked
        if (!prefs.getBoolean(activityName, true)) {
            return true;
        }
        // App unlocked
        if (System.currentTimeMillis() - get(packageName + FLAG_TMP) <= 800) {
            return true;
        }
        // I.Mod active
        boolean iModDelayGlobalEnabled = prefs.getBoolean(Common.ENABLE_IMOD_DELAY_GLOBAL, false);
        boolean iModDelayAppEnabled = prefs.getBoolean(Common.ENABLE_IMOD_DELAY_APP, false);
        long iModLastUnlockGlobal = get(IMOD_LAST_UNLOCK_GLOBAL);
        long iModLastUnlockApp = get(packageName + FLAG_IMOD);

        if ((iModDelayGlobalEnabled && (iModLastUnlockGlobal != 0 &&
                System.currentTimeMillis() - iModLastUnlockGlobal <=
                        prefs.getInt(IMOD_DELAY_GLOBAL, 600000)))
                || iModDelayAppEnabled && (iModLastUnlockApp != 0 &&
                System.currentTimeMillis() - iModLastUnlockApp <=
                        prefs.getInt(IMOD_DELAY_APP, 600000))) {
            return true;
        }
        // Activity got launched/closed
        JSONObject jsonObject = readFile(HISTORY_PATH);
        JSONArray array = jsonObject.optJSONArray(HISTORY_ARRAY_KEY);
        if (array == null) {
            array = new JSONArray();
        }
        String[] history = new String[]{array.optString(0), array.optString(1), array.optString(2)};
        return history[0].equals(history[1]) && !history[2].equals(history[0]);
    }

    public static void put(@NonNull String... arguments) throws Throwable {
        JSONObject jsonObject = readFile(Main.TEMPS_PATH);
        for (String s : arguments) {
            jsonObject.put(s, System.currentTimeMillis());
        }
        writeFile(Main.TEMPS_PATH, jsonObject);
    }

    private static long get(@NonNull String argument) throws Throwable {
        return readFile(Main.TEMPS_PATH).optLong(argument);
    }

    private static JSONObject readFile(String path) throws Throwable {
        String json;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            json = reader.readLine();
            reader.close();
            if (json == null || !json.startsWith("{")) {
                log("ML: File empty or malformed!");
                json = "{}";
            }
        } catch (FileNotFoundException e) {
            log("ML: File not found.");
            json = "{}";
        }
        return new JSONObject(json);
    }

    private static void writeFile(String path, JSONObject jsonObject) throws Throwable {
        try {
            File JSONFile = new File(path);
            if (!JSONFile.exists()) {
                throw new FileNotFoundException("File could not be written, as it doesn't exist.");
            }
            FileWriter fw = new FileWriter(path);
            fw.write(jsonObject.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addToHistory(@NonNull String activity) throws Throwable {
        JSONObject jsonObject = readFile(HISTORY_PATH);
        JSONArray array = jsonObject.optJSONArray(HISTORY_ARRAY_KEY);
        if (array == null) {
            array = new JSONArray();
        }
        array.put(2, array.optString(1));
        array.put(1, array.optString(0));
        array.put(0, activity);
        jsonObject.put(HISTORY_ARRAY_KEY, array);
        writeFile(HISTORY_PATH, jsonObject);
    }
}
