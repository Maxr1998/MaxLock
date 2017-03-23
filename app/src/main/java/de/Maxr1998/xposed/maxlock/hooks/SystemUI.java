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

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.CLOSE_OBJECT_KEY;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.IMOD_RESET_ON_SCREEN_OFF;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.PACKAGE_HISTORY_ARRAY_KEY;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.PROCESS_HISTORY_ARRAY_KEY;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.getDefault;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.readFile;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.writeFile;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

class SystemUI {

    static final String PACKAGE_NAME = "com.android.systemui";
    static final String PACKAGE_NAME_KEYGUARD = "com.android.keyguard";
    private static final String HIDE_RECENTS_THUMBNAILS = "hide_recents_thumbnails";
    private static final boolean LOLLIPOP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    static void init(final XSharedPreferences prefsApps, XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            @ColorInt final int color = true ? Color.WHITE : Color.BLACK; // TODO
            if (LOLLIPOP) {
                findAndHookMethod(PACKAGE_NAME + ".recents.views.TaskViewThumbnail", lPParam.classLoader, "rebindToTask", PACKAGE_NAME + ".recents.model.Task", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        prefsApps.reload();
                        Object task = param.args[0];
                        String packageName = ((ComponentName) getObjectField(getObjectField(getObjectField(task, "key"), "mComponentNameKey"), "component")).getPackageName();
                        if (prefsApps.getBoolean(HIDE_RECENTS_THUMBNAILS, false) && prefsApps.getBoolean(packageName, false)) {
                            Bitmap replacement;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                replacement = Bitmap.createBitmap(new int[]{color}, 1, 1, Bitmap.Config.RGB_565);
                            } else {
                                Bitmap thumbnail = ((Bitmap) getObjectField(task, "thumbnail"));
                                replacement = Bitmap.createBitmap(thumbnail.getWidth(), thumbnail.getHeight(), Bitmap.Config.RGB_565);
                                replacement.eraseColor(color);
                            }
                            setObjectField(task, "thumbnail", replacement);
                        }
                    }
                });
            } else {
                findAndHookMethod(PACKAGE_NAME + ".recent.TaskDescription", lPParam.classLoader, "getThumbnail", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        prefsApps.reload();
                        if (prefsApps.getBoolean(HIDE_RECENTS_THUMBNAILS, false) && prefsApps.getBoolean(getObjectField(param.thisObject, "packageName").toString(), false)) {
                            param.setResult(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? new ColorDrawable(color) : Bitmap.createBitmap(new int[]{color}, 1, 1, Bitmap.Config.RGB_565));
                        }
                    }
                });
            }
        } catch (Throwable t) {
            log(t);
        }
    }

    static void initScreenOff(final XSharedPreferences prefsApps, final XC_LoadPackage.LoadPackageParam lPParam) {
        // Resolve vars
        String hookedClass;
        try {
            if (LOLLIPOP) throw new Error();
            // Handle MIUI Keyguard class
            XposedHelpers.findClass(PACKAGE_NAME_KEYGUARD + ".MiuiKeyguardViewMediator", lPParam.classLoader);
            hookedClass = PACKAGE_NAME_KEYGUARD + ".MiuiKeyguardViewMediator";
            log("ML: Recognized MIUI device.");
        } catch (Error e) {
            hookedClass = lPParam.packageName.replace(".keyguard", "") + ".keyguard.KeyguardViewMediator";
        }

        final XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                prefsApps.reload();
                if (prefsApps.getBoolean(IMOD_RESET_ON_SCREEN_OFF, false)) {
                    writeFile(getDefault());
                    log("ML: Screen turned off, locked apps.");
                } else {
                    writeFile(readFile()
                            .put(PROCESS_HISTORY_ARRAY_KEY, new JSONArray())
                            .put(PACKAGE_HISTORY_ARRAY_KEY, new JSONArray())
                            .put(CLOSE_OBJECT_KEY, new JSONObject()));
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