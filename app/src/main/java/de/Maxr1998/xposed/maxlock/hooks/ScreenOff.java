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

public class ScreenOff {

    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String PACKAGE_NAME_LEGACY = "com.android.keyguard";
    public static final String IMOD_RESET_ON_SCREEN_OFF = "reset_imod_screen_off";

    public static void init(final XSharedPreferences prefsApps, final XC_LoadPackage.LoadPackageParam lPParam) {
        String hookedClass;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            paramsAndHook = new Object[]{hook};
        } else {
            paramsAndHook = new Object[]{int.class, hook};
        }
        try {
            findAndHookMethod(hookedClass, lPParam.classLoader, "onScreenTurnedOff", paramsAndHook);
        } catch (Throwable t) {
            log(t);
        }
    }
}
