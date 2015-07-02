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

package de.Maxr1998.xposed.maxlock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.Maxr1998.xposed.maxlock.LockHelper.launchLockView;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    public static final String MY_PACKAGE_NAME = Main.class.getPackage().getName();
    public static final Set<String> NO_UNLOCK = new HashSet<>(Arrays.asList(new String[]{
            "com.android.camera.CameraActivity",
            "com.evernote.ui.HomeActivity",
            "com.fstop.photo.MainActivity",
            "com.instagram",
            "com.twitter.android.StartActivity",
            "com.UCMobile.main.UCMobile",
            "com.viber.voip.WelcomeActivity",
            "com.whatsapp.Main",
            "cum.whatsfapp.Main",
            "jp.co.johospace.jorte.MainActivity",
            "se.feomedia.quizkampen.act.login.MainActivity"
    }));
    private static XSharedPreferences PREFS_APPS, PREFS_TEMP/*, PREFS_IMOD*/;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("Loaded class Main @ MaxLock.");
        PREFS_APPS = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_APPS);
        PREFS_TEMP = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_TEMP);
        //PREFS_IMOD = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_IMOD);
        makeReadable();
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lPParam) throws Throwable {
        final String packageName = lPParam.packageName;
        if (!PREFS_APPS.getBoolean(packageName, false)) {
            return;
        }

        findAndHookMethod("android.app.Activity", lPParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("MLaC|" + param.thisObject.getClass().getName() + "|-|" + System.currentTimeMillis());
            }
        });

        findAndHookMethod("android.app.Activity", lPParam.classLoader, "onStart", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                reload();
                Activity app = (Activity) param.thisObject;
                if ((System.currentTimeMillis() - PREFS_TEMP.getLong(packageName + Common.FLAG_CLOSE_APP, 0)) <= 800) {
                    app.finish();
                    return;
                }
                long unlockTimestamp = Math.max(PREFS_TEMP.getLong(packageName + Common.FLAG_TMP, 0), PreferenceManager.getDefaultSharedPreferences(app).getLong("MaxLockLastUnlock", 0));
                if (System.currentTimeMillis() - unlockTimestamp <= 600) {
                    return;
                }
                /*if (timerOrIMod(packageName, unlockTimestamp, PREFS_IMOD, PREFS_TEMP)) {
                    return;
                }*/
                if (app.getClass().getName().equals("android.app.Activity") ||
                        !PREFS_APPS.getBoolean(Common.MASTER_SWITCH_ON, true) ||
                        !PREFS_APPS.getBoolean(app.getClass().getName(), true) ||
                        NO_UNLOCK.contains(app.getClass().getName())) {
                    return;
                }
                launchLockView(app, app.getIntent(), packageName, PREFS_APPS.getBoolean(packageName + "_fake", false));
            }
        });

        // Handling back action
        findAndHookMethod("android.app.Activity", lPParam.classLoader, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                PreferenceManager.getDefaultSharedPreferences((Activity) param.thisObject).edit().putLong("MaxLockLastUnlock", System.currentTimeMillis()).commit();
            }
        });

        // Handling activity starts inside package
        findAndHookMethod("android.app.Instrumentation", lPParam.classLoader, "execStartActivity",
                Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        boolean set = false;
                        if (!((Intent) param.args[4]).getComponent().getPackageName().equals(MY_PACKAGE_NAME) &&
                                !NO_UNLOCK.contains(param.args[0].getClass().getName()) &&
                                PREFS_APPS.getBoolean(param.args[0].getClass().getName(), true)) {
                            PreferenceManager.getDefaultSharedPreferences((Context) param.args[0]).edit().putLong("MaxLockLastUnlock", System.currentTimeMillis()).commit();
                            set = true;
                        }
                        XposedBridge.log("MLi" + (set ? "U" : "N") + "|" + param.args[0].getClass().getName() + "|" + ((Intent) param.args[4]).getComponent().getClassName() + "|" + System.currentTimeMillis());
                    }
                });
    }

    private void makeReadable() {
        PREFS_APPS.makeWorldReadable();
        PREFS_TEMP.makeWorldReadable();
        /*PREFS_IMOD.makeWorldReadable();*/
    }

    private void reload() {
        PREFS_APPS.reload();
        PREFS_TEMP.reload();
        /*PREFS_IMOD.reload();*/
    }
}
