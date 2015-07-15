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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Apps {

    public static final String PACKAGE_NAME = "de.Maxr1998.xposed.maxlock";
    public static final String FLAG_CLOSE_APP = "_close";
    public static final String FLAG_TMP = "_tmp";
    public static final String FLAG_IMOD = "_imod";
    public static final String IMOD_DELAY_APP = "delay_inputperapp";
    public static final String IMOD_DELAY_GLOBAL = "delay_inputgeneral";
    public static final String IMOD_LAST_UNLOCK_GLOBAL = "IMoDGlobalDelayTimer";

    private static final Set<String> NO_UNLOCK = new HashSet<>(Arrays.asList(new String[]{
            "com.android.camera.CameraActivity",
            "com.evernote.ui.HomeActivity",
            "com.facebook.nodex.startup.splashscreen.NodexSplashActivity",
            "com.fstop.photo.MainActivity",
            "com.instagram",
            "com.tumblr.ui.activity.JumpoffActivity",
            "com.twitter.android.StartActivity",
            "com.UCMobile.main.UCMobile",
            "com.viber.voip.WelcomeActivity",
            "com.whatsapp.Main",
            "cum.whatsfapp.Main",
            "jp.co.johospace.jorte.MainActivity",
            "jp.naver.line.android.activity.SplashActivity",
            "se.feomedia.quizkampen.act.login.MainActivity"
    }));

    public static void init(final XSharedPreferences prefsApps, final XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod("android.app.Activity", lPParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!param.thisObject.getClass().getName().equals("android.app.Activity")) {
                        log("MLaC|" + param.thisObject.getClass().getName() + "|-|" + System.currentTimeMillis());
                    }
                }
            });

            findAndHookMethod("android.app.Activity", lPParam.classLoader, "onStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefsApps.reload();
                    Activity app = (Activity) param.thisObject;
                    if (System.currentTimeMillis() - get(lPParam.packageName + FLAG_CLOSE_APP) <= 800) {
                        app.finish();
                        return;
                    }
                    if (app.getClass().getName().equals("android.app.Activity") ||
                            !prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true) ||
                            timerOrIMod(lPParam.packageName, prefsApps) ||
                            !prefsApps.getBoolean(app.getClass().getName(), true) ||
                            NO_UNLOCK.contains(app.getClass().getName())) {
                        return;
                    }
                    Intent it = new Intent();
                    it.setComponent(new ComponentName(PACKAGE_NAME, PACKAGE_NAME + ".ui.LockActivity"));
                    it.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    it.putExtra(Common.LOCK_ACTIVITY_MODE, prefsApps.getBoolean(lPParam.packageName + "_fake", false) ? Common.MODE_FAKE_DIE : Common.MODE_DEFAULT);
                    it.putExtra(Common.INTENT_EXTRAS_INTENT, app.getIntent());
                    it.putExtra(Common.INTENT_EXTRAS_PKG_NAME, lPParam.packageName);
                    app.startActivity(it);
                }
            });

            // Handling back action
            findAndHookMethod("android.app.Activity", lPParam.classLoader, "onPause", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!param.thisObject.getClass().getName().equals("android.app.Activity")) {
                        put(lPParam.packageName + FLAG_TMP);
                    }
                }
            });

            // Handling activity starts inside package
            findAndHookMethod("android.app.Instrumentation", lPParam.classLoader, "execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            prefsApps.reload();
                            if (param.thisObject.getClass().getName().equals("android.app.Activity")) {
                                return;
                            }
                            boolean set = false;
                            if (!((Intent) param.args[4]).getComponent().getPackageName().equals(PACKAGE_NAME) &&
                                    !NO_UNLOCK.contains(param.args[0].getClass().getName()) &&
                                    prefsApps.getBoolean(param.args[0].getClass().getName(), true)) {
                                put(lPParam.packageName + FLAG_TMP);
                                set = true;
                            }
                            log("MLi" + (set ? "U" : "N") + "|" + param.args[0].getClass().getName() + "|" + ((Intent) param.args[4]).getComponent().getClassName() + "|" + System.currentTimeMillis());
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void put(final String... arguments) throws Throwable {
        String json;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Main.TEMPS_PATH));
            json = reader.readLine();
            reader.close();
        } catch (FileNotFoundException e) {
            json = "";
        }
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            jsonObject = new JSONObject();
        }
        for (String s : arguments) {
            jsonObject.put(s, System.currentTimeMillis());
        }
        try {
            File JSONFile = new File(Main.TEMPS_PATH);
            if (!JSONFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                JSONFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                JSONFile.createNewFile();
            }
            FileWriter fw = new FileWriter(Main.TEMPS_PATH);
            fw.write(jsonObject.toString());
            fw.close();
        } catch (IOException e) {
            // Do nothing.
        }
    }

    private static long get(String argument) throws Throwable {
        String json;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Main.TEMPS_PATH));
            json = reader.readLine();
            reader.close();
        } catch (FileNotFoundException e) {
            json = "";
        }
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            jsonObject = new JSONObject();
        }
        return jsonObject.optLong(argument);
    }

    private static boolean timerOrIMod(String packageName, XSharedPreferences prefsImod) throws Throwable {
        if (System.currentTimeMillis() - get(packageName + FLAG_TMP) <= 800) {
            return true;
        }
        // Intika I.MoD
        boolean iModDelayGlobalEnabled = prefsImod.getBoolean("enable_delaygeneral", false);
        boolean iModDelayAppEnabled = prefsImod.getBoolean("enable_delayperapp", false);
        long iModLastUnlockGlobal = get(IMOD_LAST_UNLOCK_GLOBAL);
        long iModLastUnlockApp = get(packageName + FLAG_IMOD);

        return (iModDelayGlobalEnabled && (iModLastUnlockGlobal != 0 &&
                System.currentTimeMillis() - iModLastUnlockGlobal <=
                        prefsImod.getInt(IMOD_DELAY_GLOBAL, 600000)))
                || iModDelayAppEnabled && (iModLastUnlockApp != 0 &&
                System.currentTimeMillis() - iModLastUnlockApp <=
                        prefsImod.getInt(IMOD_DELAY_APP, 600000));
    }
}
