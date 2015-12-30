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

import android.annotation.TargetApi;
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
import static de.Maxr1998.xposed.maxlock.hooks.Apps.HISTORY_ARRAY_KEY;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.getDefault;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.readFile;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.writeFile;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class SystemUI {

    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String PACKAGE_NAME_LEGACY = "com.android.keyguard";
    public static final String IMOD_RESET_ON_SCREEN_OFF = "reset_imod_screen_off";

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void init(XSharedPreferences prefsApps, XC_LoadPackage.LoadPackageParam lPParam) {
        initRecents(prefsApps, lPParam);
        initScreenOff(prefsApps, lPParam, true);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void initRecents(final XSharedPreferences prefsApps, XC_LoadPackage.LoadPackageParam lPParam) {
        try {
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
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void initRecentsLegacy(final XSharedPreferences prefsApps, XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod(PACKAGE_NAME + ".recent.TaskDescription", lPParam.classLoader, "getThumbnail", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefsApps.reload();
                    if (prefsApps.getBoolean("hide_recents_thumbnails", false) && prefsApps.getBoolean(getObjectField(param.thisObject, "packageName").toString(), false)) {
                        param.setResult(new ColorDrawable(Color.WHITE));
                    }
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void initScreenOff(final XSharedPreferences prefsApps, final XC_LoadPackage.LoadPackageParam lPParam, boolean lollipop) {
        // Resolve vars
        String hookedClass;
        if (lollipop) {
            hookedClass = "com.android.systemui.keyguard.KeyguardViewMediator";
        } else {
            try {
                XposedHelpers.findClass("com.android.keyguard.MiuiKeyguardViewMediator", lPParam.classLoader);
                hookedClass = "com.android.keyguard.MiuiKeyguardViewMediator";
                log("ML: Recognized MIUI device.");
            } catch (XposedHelpers.ClassNotFoundError e) {
                hookedClass = "com.android.keyguard.KeyguardViewMediator";
            }
        }
        final XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                prefsApps.reload();
                if (prefsApps.getBoolean(IMOD_RESET_ON_SCREEN_OFF, false)) {
                    writeFile(getDefault());
                    log("ML: Screen turned off, locked apps.");
                } else {
                    writeFile(readFile().put(HISTORY_ARRAY_KEY, new JSONArray()).put(CLOSE_OBJECT_KEY, new JSONObject()));
                }
            }
        };
        Object[] paramsAndHook;
        if (lollipop && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            paramsAndHook = new Object[]{hook};
        } else {
            paramsAndHook = new Object[]{int.class, hook};
        }

        // Hook
        try {
            findAndHookMethod(hookedClass, lPParam.classLoader, "onScreenTurnedOff", paramsAndHook);
        } catch (Throwable t) {
            log(t);
        }
    }
}
