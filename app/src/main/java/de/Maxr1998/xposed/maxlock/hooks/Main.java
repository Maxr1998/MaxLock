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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.Maxr1998.xposed.maxlock.Common.TEMPS_FILE;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    public static final String MY_PACKAGE_NAME = "de.Maxr1998.xposed.maxlock";
    public static final Set<String> NO_UNLOCK = new HashSet<>(Arrays.asList(new String[]{
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
    public static XSharedPreferences PREFS_APPS, PREFS_IMOD;

    public static void put(final String... arguments) throws Throwable {
        String json;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(TEMPS_FILE));
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
        File JSONFile = new File(TEMPS_FILE);
        if (!JSONFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            JSONFile.getParentFile().mkdirs();
            //noinspection ResultOfMethodCallIgnored
            JSONFile.createNewFile();
        }
        FileWriter fw = new FileWriter(JSONFile.getAbsoluteFile());
        fw.write(jsonObject.toString());
        fw.close();
    }

    private static long get(String argument) throws Throwable {
        String json;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(TEMPS_FILE));
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

    public static void clear() throws Throwable {
        FileWriter fw = new FileWriter(new File(TEMPS_FILE).getAbsoluteFile());
        fw.write("{\"TheAnswer\":\"42\"}");
        fw.close();
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("Loaded class Main @ MaxLock.");
        PREFS_APPS = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_APPS);
        PREFS_IMOD = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_IMOD);
        makeReadable();
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lPParam) throws Throwable {
        final String packageName = lPParam.packageName;
        reloadPrefs();
        if (!PREFS_APPS.getBoolean(packageName, false)) {
            return;
        }

        findAndHookMethod("android.app.Activity", lPParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.getClass().getName().equals("android.app.Activity")) {
                    XposedBridge.log("MLaC|" + param.thisObject.getClass().getName() + "|-|" + System.currentTimeMillis());
                }
            }
        });

        findAndHookMethod("android.app.Activity", lPParam.classLoader, "onStart", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                reloadPrefs();
                Activity app = (Activity) param.thisObject;
                if (System.currentTimeMillis() - get(packageName + Common.FLAG_CLOSE_APP) <= 800) {
                    app.finish();
                    return;
                }
                if (app.getClass().getName().equals("android.app.Activity") ||
                        !PREFS_APPS.getBoolean(Common.MASTER_SWITCH_ON, true) ||
                        timerOrIMod(packageName) ||
                        !PREFS_APPS.getBoolean(app.getClass().getName(), true) ||
                        NO_UNLOCK.contains(app.getClass().getName())) {
                    return;
                }
                Intent it = new Intent();
                it.setComponent(new ComponentName(MY_PACKAGE_NAME, MY_PACKAGE_NAME + ".ui.LockActivity"));
                it.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                it.putExtra(Common.LOCK_ACTIVITY_MODE, PREFS_APPS.getBoolean(packageName + "_fake", false) ? Common.MODE_FAKE_DIE : Common.MODE_DEFAULT);
                it.putExtra(Common.INTENT_EXTRAS_INTENT, app.getIntent());
                it.putExtra(Common.INTENT_EXTRAS_PKG_NAME, packageName);
                app.startActivity(it);
            }
        });

        // Handling back action
        findAndHookMethod("android.app.Activity", lPParam.classLoader, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.getClass().getName().equals("android.app.Activity")) {
                    put(packageName + Common.FLAG_TMP);
                }
            }
        });

        // Handling activity starts inside package
        findAndHookMethod("android.app.Instrumentation", lPParam.classLoader, "execStartActivity",
                Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.thisObject.getClass().getName().equals("android.app.Activity")) {
                            return;
                        }
                        boolean set = false;
                        if (!((Intent) param.args[4]).getComponent().getPackageName().equals(MY_PACKAGE_NAME) &&
                                !NO_UNLOCK.contains(param.args[0].getClass().getName()) &&
                                PREFS_APPS.getBoolean(param.args[0].getClass().getName(), true)) {
                            put(packageName + Common.FLAG_TMP);
                            set = true;
                        }
                        XposedBridge.log("MLi" + (set ? "U" : "N") + "|" + param.args[0].getClass().getName() + "|" + ((Intent) param.args[4]).getComponent().getClassName() + "|" + System.currentTimeMillis());
                    }
                });
    }

    private void makeReadable() {
        PREFS_APPS.makeWorldReadable();
        PREFS_IMOD.makeWorldReadable();
    }

    private void reloadPrefs() {
        PREFS_APPS.reload();
        PREFS_IMOD.reload();
    }

    private boolean timerOrIMod(String packageName) throws Throwable {
        if (System.currentTimeMillis() - get(packageName + Common.FLAG_TMP) <= 800) {
            return true;
        }
        // Intika I.MoD
        boolean iModDelayGlobalEnabled = PREFS_IMOD.getBoolean(Common.IMOD_DELAY_GLOBAL_ENABLED, false);
        boolean iModDelayAppEnabled = PREFS_IMOD.getBoolean(Common.IMOD_DELAY_APP_ENABLED, false);
        long iModLastUnlockGlobal = get(Common.IMOD_LAST_UNLOCK_GLOBAL);
        long iModLastUnlockApp = get(packageName + Common.FLAG_IMOD);

        return (iModDelayGlobalEnabled && (iModLastUnlockGlobal != 0 &&
                System.currentTimeMillis() - iModLastUnlockGlobal <=
                        PREFS_IMOD.getInt(Common.IMOD_DELAY_GLOBAL, 600000)))
                || iModDelayAppEnabled && (iModLastUnlockApp != 0 &&
                System.currentTimeMillis() - iModLastUnlockApp <=
                        PREFS_IMOD.getInt(Common.IMOD_DELAY_APP, 600000));
    }
}
