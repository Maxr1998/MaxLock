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
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
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

import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.hooks.Main.MAXLOCK_PACKAGE_NAME;
import static de.Maxr1998.xposed.maxlock.hooks.Main.logD;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class Apps {

    @SuppressLint("SdCardPath")
    public static final String HISTORY_PATH = "/data/data/" + Main.MAXLOCK_PACKAGE_NAME + "/files/history.json";
    public static final String PROCESS_HISTORY_ARRAY_KEY = "procs";
    public static final String PACKAGE_HISTORY_ARRAY_KEY = "pkgs";
    public static final int UNLOCK_ID = -0x3A8;
    public static final String IMOD_OBJECT_KEY = "iModPerApp";
    public static final String CLOSE_OBJECT_KEY = "close";

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
                    final Activity activity = (Activity) param.thisObject;
                    String activityName = activity.getClass().getName();
                    logD("ML|Started " + activityName + " at " + System.currentTimeMillis());
                    JSONObject history = readFile();
                    JSONObject close = history.optJSONObject(CLOSE_OBJECT_KEY);
                    if (close != null && System.currentTimeMillis() - close.optLong(lPParam.packageName) <= 800) {
                        activity.finish();
                        logD("ML|Finish " + activityName);
                        return;
                    }
                    prefsApps.reload();
                    if (activityName.equals("android.app.Activity") ||
                            pass(activity.getTaskId(), lPParam.packageName, activityName, history, prefsApps)) {
                        return;
                    }
                    Intent it = new Intent();
                    it.setComponent(new ComponentName(Main.MAXLOCK_PACKAGE_NAME, Main.MAXLOCK_PACKAGE_NAME + ".ui.LockActivity"));
                    it.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    if (prefsApps.getBoolean(lPParam.packageName + "_fake", false)) {
                        it.putExtra(Common.LOCK_ACTIVITY_MODE, Common.MODE_FAKE_CRASH);
                    }
                    it.putExtra(Common.INTENT_EXTRAS_NAMES, new String[]{lPParam.packageName, activityName});
                    activity.startActivity(it);
                }
            });
            findAndHookMethod("android.app.Activity", lPParam.classLoader, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((boolean) param.args[0]) {
                        JSONObject history = readFile();
                        JSONObject close = history.optJSONObject(CLOSE_OBJECT_KEY);
                        if (close != null && System.currentTimeMillis() - close.optLong(lPParam.packageName) <= 800) {
                            final Activity activity = (Activity) param.thisObject;
                            activity.finish();
                            logD("ML|Finish " + activity.getClass().getName());
                        }
                    }
                }
            });

            findAndHookMethod(NotificationManager.class, "notify", String.class, int.class, Notification.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefsApps.reload();
                    Notification notification = (Notification) param.args[2];
                    if (prefsApps.getBoolean(lPParam.packageName + "_notif_content", false)) {
                        Context context = (Context) getObjectField(param.thisObject, "mContext");
                        String appName = context.getPackageManager().getApplicationInfo(lPParam.packageName, 0).loadLabel(context.getPackageManager()).toString();
                        Resources modRes = context.getPackageManager().getResourcesForApplication(MAXLOCK_PACKAGE_NAME);
                        String replacement = modRes.getString(modRes.getIdentifier("notification_hidden_by_maxlock", "string", MAXLOCK_PACKAGE_NAME));
                        Notification.Builder b = new Notification.Builder(context).setContentTitle(appName).setContentText(replacement);
                        notification.contentView = b.build().contentView;
                        notification.bigContentView = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            notification.headsUpContentView = null;
                        notification.tickerText = replacement;
                    }
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }

    private static boolean pass(int taskId, @NonNull String packageName, String activityName, @NonNull JSONObject history, @NonNull XSharedPreferences prefs) throws Throwable {
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
            iModLastUnlockApp = iModPerApp.optLong(packageName);
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
            if (!(history.has(PROCESS_HISTORY_ARRAY_KEY) && history.has(PACKAGE_HISTORY_ARRAY_KEY) && history.has(IMOD_OBJECT_KEY) && history.has(CLOSE_OBJECT_KEY))) {
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
        JSONArray procs = history.optJSONArray(PROCESS_HISTORY_ARRAY_KEY);
        JSONArray pkgs = history.optJSONArray(PACKAGE_HISTORY_ARRAY_KEY);
        // Only add task id if new task got launched
        if (taskId != procs.optInt(0)) {
            // If new task doesn't have same package name, keep (shift back) the previous task id
            if (!packageName.equals(pkgs.optString(0))) {
                procs.put(1, procs.optInt(0));
            }
            procs.put(0, taskId);
        }
        pkgs
                .put(2, pkgs.optString(1))
                .put(1, pkgs.optString(0))
                .put(0, packageName);

        history.put(PROCESS_HISTORY_ARRAY_KEY, procs).put(PACKAGE_HISTORY_ARRAY_KEY, pkgs);
        return history;
    }
}