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

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.util.ColorSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.IMOD_RESET_ON_SCREEN_OFF;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.trim;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

class SystemUI {

    static final String PACKAGE_NAME = "com.android.systemui";
    static final String PACKAGE_NAME_KEYGUARD = "com.android.keyguard";
    private static final String HIDE_RECENTS_THUMBNAILS = "hide_recents_thumbnails";
    private static final boolean LOLLIPOP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    private static final boolean MARSHMALLOW = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    private static final boolean OREO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

    static void init(XC_LoadPackage.LoadPackageParam lPParam, final SharedPreferences prefs, final SharedPreferences prefsApps) {
        try {
            ColorSupplier color = () -> !prefs.getBoolean(Common.INVERT_COLOR, false) ? Color.WHITE : Color.BLACK;
            if (LOLLIPOP) {
                hookAllMethods(findClass(PACKAGE_NAME + ".recents.model.Task", lPParam.classLoader), "notifyTaskDataLoaded", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String packageName = ((Intent) getObjectField(getObjectField(param.thisObject, "key"), "baseIntent")).getComponent().getPackageName();
                        if (prefs.getBoolean(HIDE_RECENTS_THUMBNAILS, false) && prefsApps.getBoolean(packageName, false)) {
                            Bitmap replacement;
                            if (MARSHMALLOW) {
                                replacement = Bitmap.createBitmap(new int[]{color.getAsColor()}, 1, 1, Bitmap.Config.RGB_565);
                            } else {
                                Bitmap thumbnail = (Bitmap) param.args[0];
                                replacement = Bitmap.createBitmap(thumbnail.getWidth(), thumbnail.getHeight(), Bitmap.Config.RGB_565);
                                replacement.eraseColor(color.getAsColor());
                            }
                            if (OREO) {
                                setObjectField(param.args[0], "thumbnail", replacement);
                            } else param.args[0] = replacement;
                        }
                    }
                });
            } else {
                findAndHookMethod(PACKAGE_NAME + ".recent.TaskDescription", lPParam.classLoader, "getThumbnail", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (prefs.getBoolean(HIDE_RECENTS_THUMBNAILS, false) && prefsApps.getBoolean(getObjectField(param.thisObject, "packageName").toString(), false)) {
                            param.setResult(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? new ColorDrawable(color.getAsColor()) : Bitmap.createBitmap(new int[]{color.getAsColor()}, 1, 1, Bitmap.Config.RGB_565));
                        }
                    }
                });
            }
        } catch (Throwable t) {
            log(t);
        }
    }

    static void initScreenOff(final XC_LoadPackage.LoadPackageParam lPParam, final SharedPreferences prefsApps, final SharedPreferences prefsHistory) {
        // Resolve vars
        String hookedClass;
        try {
            if (LOLLIPOP) throw new Error();
            // Handle MIUI Keyguard class
            findClass(PACKAGE_NAME_KEYGUARD + ".MiuiKeyguardViewMediator", lPParam.classLoader);
            hookedClass = PACKAGE_NAME_KEYGUARD + ".MiuiKeyguardViewMediator";
            log("ML: Recognized MIUI device.");
        } catch (Error e) {
            hookedClass = lPParam.packageName.replace(".keyguard", "") + ".keyguard.KeyguardViewMediator";
        }

        final XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefsApps.getBoolean(IMOD_RESET_ON_SCREEN_OFF, false)) {
                    prefsHistory.edit().clear().apply();
                    log("ML: Screen turned off, locked apps.");
                } else {
                    trim(prefsHistory);
                }
            }
        };
        // Hook
        try {
            findAndHookMethod(hookedClass, lPParam.classLoader, "onScreenTurnedOff", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? new Object[]{hook} : new Object[]{int.class, hook});
        } catch (Throwable t) {
            log(t);
        }
    }
}