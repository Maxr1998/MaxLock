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
    private static XSharedPreferences PREFS_PACKAGES, PREFS_ACTIVITIES;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        PREFS_PACKAGES = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_PACKAGES);
        PREFS_ACTIVITIES = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREFS_ACTIVITIES);
        makeReadable();
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        makeReadable();
        final String packageName = lpparam.packageName;
        if (!PREFS_PACKAGES.getBoolean(packageName, false)) {
            return;
        }

        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log(param.thisObject.getClass().getName() + " created at " + System.currentTimeMillis());
            }
        });

        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onStart", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                reload();
                final Activity app = (Activity) param.thisObject;

                Long unlockTimestamp = Math.max(PREFS_PACKAGES.getLong(packageName + "_tmp", 0), PreferenceManager.getDefaultSharedPreferences(app).getLong("MaxLockLastUnlock", 0));
                if (!PREFS_PACKAGES.getBoolean(packageName, false) || (unlockTimestamp != 0 && System.currentTimeMillis() - unlockTimestamp <= 500)) {
                    return;
                }
                if (app.getClass().getName().equals("android.app.Activity") ||
                        !PREFS_PACKAGES.getBoolean(Common.MASTER_SWITCH_ON, true) ||
                        !PREFS_ACTIVITIES.getBoolean(app.getClass().getName(), true)) {
                    return;
                }
                app.moveTaskToBack(true);
                launchLockView(app, app.getIntent(), packageName, PREFS_PACKAGES.getBoolean(packageName + "_fake", false) ? ".ui.FakeDieDialog" : ".ui.LockActivity");
            }
        });

        // Handling back action
        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                PreferenceManager.getDefaultSharedPreferences((Activity) param.thisObject).edit().putLong("MaxLockLastUnlock", System.currentTimeMillis()).commit();
            }
        });

        // Handling activity starts inside package
        findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "execStartActivity",
                Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        boolean set = false;
                        if (!((Intent) param.args[4]).getComponent().getPackageName().equals(MY_PACKAGE_NAME) && !LockHelper.NO_UNLOCK.contains(param.args[0].getClass().getName())) {
                            PreferenceManager.getDefaultSharedPreferences((Context) param.args[0]).edit().putLong("MaxLockLastUnlock", System.currentTimeMillis()).commit();
                            set = true;
                        }
                        XposedBridge.log(param.args[0].getClass().getName() + " started Intent " + ((Intent) param.args[4]).getComponent().getClassName() + " at " + System.currentTimeMillis() + ", " + (set ? "" : "not ") + "unlocked.");
                    }
                });
    }

    private void makeReadable() {
        PREFS_PACKAGES.makeWorldReadable();
        PREFS_ACTIVITIES.makeWorldReadable();
        reload();
    }

    private void reload() {
        PREFS_PACKAGES.reload();
        PREFS_ACTIVITIES.reload();
    }
}