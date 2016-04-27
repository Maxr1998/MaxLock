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

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.hooks.Apps.CLOSE_OBJECT_KEY;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.PACKAGE_HISTORY_ARRAY_KEY;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.PROCESS_HISTORY_ARRAY_KEY;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.getDefault;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.readFile;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.writeFile;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class SystemUI {

    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String PACKAGE_NAME_KEYGUARD = "com.android.keyguard";
    public static final String IMOD_RESET_ON_SCREEN_OFF = "reset_imod_screen_off";
    private static final boolean LOLLIPOP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    public static void init(final XSharedPreferences prefsApps, XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            if (LOLLIPOP) {
                findAndHookMethod(PACKAGE_NAME + ".recents.views.TaskViewThumbnail", lPParam.classLoader, "rebindToTask", PACKAGE_NAME + ".recents.model.Task", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        prefsApps.reload();
                        Object task = param.args[0];
                        String packageName = ((ComponentName) getObjectField(getObjectField(getObjectField(task, "key"), "mComponentNameKey"), "component")).getPackageName();
                        if (prefsApps.getBoolean("hide_recents_thumbnails", false) && prefsApps.getBoolean(packageName, false)) {
                            Bitmap thumbnail = ((Bitmap) getObjectField(task, "thumbnail"));
                            Bitmap replacement = Bitmap.createBitmap(thumbnail.getWidth(), thumbnail.getHeight(), Bitmap.Config.RGB_565);
                            replacement.eraseColor(Color.WHITE);
                            setObjectField(task, "thumbnail", replacement);
                        }
                    }
                });
            } else {
                findAndHookMethod(PACKAGE_NAME + ".recent.TaskDescription", lPParam.classLoader, "getThumbnail", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        prefsApps.reload();
                        if (prefsApps.getBoolean("hide_recents_thumbnails", false) && prefsApps.getBoolean(getObjectField(param.thisObject, "packageName").toString(), false)) {
                            param.setResult(new ColorDrawable(Color.WHITE));
                        }
                    }
                });
            }
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void initScreenOff(final XSharedPreferences prefsApps, final XC_LoadPackage.LoadPackageParam lPParam) {
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
            findAndHookMethod(hookedClass, lPParam.classLoader, "onScreenTurnedOff", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? hook : new Object[]{int.class, hook});
        } catch (Throwable t) {
            log(t);
        }
    }
}
