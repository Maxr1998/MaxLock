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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    public static final String MY_PACKAGE_NAME = Main.class.getPackage().getName();
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
    private static XSharedPreferences PREFS_APPS/*, PREFS_IMOD*/;

    private static HashMap<String, Long> TEMPS;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("Loaded class Main @ MaxLock.");
        PREFS_APPS = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_APPS);
        //PREFS_IMOD = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_IMOD);
        TEMPS = new HashMap<>();
        makeReadable();
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lPParam) throws Throwable {
        final String packageName = lPParam.packageName;

        if (packageName.equals(MY_PACKAGE_NAME)) {
            findAndHookMethod(MY_PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "openApp", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    TEMPS.put(XposedHelpers.getObjectField(param.thisObject, "packageName") + Common.FLAG_TMP, System.currentTimeMillis());
                }
            });
            findAndHookMethod(MY_PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onAuthenticationSucceeded", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    TEMPS.put(XposedHelpers.getObjectField(param.thisObject, "packageName") + Common.FLAG_IMOD, System.currentTimeMillis());
                    TEMPS.put(Common.IMOD_LAST_UNLOCK_GLOBAL, System.currentTimeMillis());
                }
            });
            findAndHookMethod(MY_PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onBackPressed", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    TEMPS.put(XposedHelpers.getObjectField(param.thisObject, "packageName") + Common.FLAG_CLOSE_APP, System.currentTimeMillis());
                }
            });
            findAndHookMethod(MY_PACKAGE_NAME + ".tasker.TaskActionReceiver", lPParam.classLoader, "clearImod", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    TEMPS.clear();
                }
            });
            return;
        }

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
                reload();
                Activity app = (Activity) param.thisObject;
                if (System.currentTimeMillis() - safeLong(TEMPS.get(packageName + Common.FLAG_CLOSE_APP)) <= 800) {
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
                XposedHelpers.callStaticMethod(findClass(MY_PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader), "launchLockView",
                        new Class[]{Activity.class, Intent.class, String.class, Boolean.class},
                        app, app.getIntent(), packageName, PREFS_APPS.getBoolean(packageName + "_fake", false));
            }
        });

        // Handling back action
        findAndHookMethod("android.app.Activity", lPParam.classLoader, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.getClass().getName().equals("android.app.Activity")) {
                    TEMPS.put(packageName + Common.FLAG_TMP, System.currentTimeMillis());
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
                            TEMPS.put(packageName + Common.FLAG_TMP, System.currentTimeMillis());
                            set = true;
                        }
                        XposedBridge.log("MLi" + (set ? "U" : "N") + "|" + param.args[0].getClass().getName() + "|" + ((Intent) param.args[4]).getComponent().getClassName() + "|" + System.currentTimeMillis());
                    }
                });
    }

    private void makeReadable() {
        PREFS_APPS.makeWorldReadable();
        /*PREFS_IMOD.makeWorldReadable();*/
    }

    private void reload() {
        PREFS_APPS.reload();
        /*PREFS_IMOD.reload();*/
    }

    public boolean timerOrIMod(String packageName) {
        if (System.currentTimeMillis() - safeLong(TEMPS.get(packageName + Common.FLAG_TMP)) <= 600) {
            return true;
        }

        /*// Intika I.MoD
        boolean iModDelayGlobalEnabled = PREFS_IMOD.getBoolean(Common.IMOD_DELAY_GLOBAL_ENABLED, false);
        boolean iModDelayAppEnabled = PREFS_IMOD.getBoolean(Common.IMOD_DELAY_APP_ENABLED, false);
        long iModLastUnlockGlobal = TEMPS.get(Common.IMOD_LAST_UNLOCK_GLOBAL);
        long iModLastUnlockApp = TEMPS.get(packageName + Common.FLAG_IMOD);

        return (iModDelayGlobalEnabled && (iModLastUnlockGlobal != 0 &&
                System.currentTimeMillis() - iModLastUnlockGlobal <=
                        PREFS_IMOD.getInt(Common.IMOD_DELAY_GLOBAL, 600000)))
                || iModDelayAppEnabled && (iModLastUnlockApp != 0 &&
                System.currentTimeMillis() - iModLastUnlockApp <=
                        PREFS_IMOD.getInt(Common.IMOD_DELAY_APP, 600000));*/
        return false;
    }

    private long safeLong(Long l) {
        return l == null ? 0L : l;
    }
}
