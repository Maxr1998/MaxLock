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

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.util.ColorSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.os.Build.VERSION.SDK_INT;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.IMOD_RESET_ON_SCREEN_OFF;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.iModActive;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.trim;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

@SuppressWarnings("RedundantThrows")
class SystemUI {

    static final String PACKAGE_NAME = "com.android.systemui";
    static final String PACKAGE_NAME_KEYGUARD = "com.android.keyguard";
    private static final boolean LOLLIPOP = SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    private static final boolean NOUGAT = SDK_INT >= Build.VERSION_CODES.N;

    static void init(XC_LoadPackage.LoadPackageParam lPParam, final SharedPreferences prefs, final SharedPreferences prefsApps, final SharedPreferences prefsHistory) {
        try {
            ColorSupplier color = () -> prefs.getBoolean(Common.USE_DARK_STYLE, false) ? 0xFF212121 : Color.WHITE;
            final Paint paint = new Paint();
            if (LOLLIPOP) {
                String methodName = (NOUGAT ? "" : "re") + "bindToTask";
                hookAllMethods(findClass(PACKAGE_NAME + ".recents.views.TaskViewThumbnail", lPParam.classLoader), methodName, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object task = getObjectField(param.thisObject, "mTask");
                        String packageName = ((Intent) getObjectField(getObjectField(task, "key"), "baseIntent")).getComponent().getPackageName();
                        paint.setColor(color.getAsColor());
                        ((View) param.thisObject).setTag(
                                prefs.getBoolean(Common.HIDE_RECENTS_THUMBNAILS, false) &&
                                        prefsApps.getBoolean(packageName, false) && !iModActive(packageName, prefsApps, prefsHistory)
                        );
                    }
                });
                findAndHookMethod(PACKAGE_NAME + ".recents.views.TaskViewThumbnail", lPParam.classLoader, "onDraw", Canvas.class, new XC_MethodHook() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (Boolean.TRUE.equals(((View) param.thisObject).getTag())) {
                            Canvas canvas = (Canvas) param.args[0];
                            int cornerRadius = getIntField(param.thisObject, "mCornerRadius") + 1;
                            canvas.drawRoundRect(0, 0, canvas.getWidth(), canvas.getHeight(), cornerRadius, cornerRadius, paint);
                            param.setResult(null);
                        }
                    }
                });
            } else {
                findAndHookMethod(PACKAGE_NAME + ".recent.TaskDescription", lPParam.classLoader, "getThumbnail", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String packageName = getObjectField(param.thisObject, "packageName").toString();
                        if (iModActive(packageName, prefsApps, prefsHistory))
                            return;
                        if (prefs.getBoolean(Common.HIDE_RECENTS_THUMBNAILS, false) && prefsApps.getBoolean(packageName, false)) {
                            param.setResult(new ColorDrawable(color.getAsColor()));
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
            protected void beforeHookedMethod(MethodHookParam param) {
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
            findAndHookMethod(hookedClass, lPParam.classLoader, "onScreenTurnedOff", SDK_INT >= Build.VERSION_CODES.M ? new Object[]{hook} : new Object[]{int.class, hook});
        } catch (Throwable t) {
            log(t);
        }
    }
}