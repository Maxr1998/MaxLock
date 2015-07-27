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

import java.io.FileWriter;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class ScreenOff {

    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String IMOD_RESET_ON_SCREEN_OFF = "reset_imod_screen_off";

    public static void init(final XSharedPreferences prefsApps, final XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? "com.android.systemui.keyguard.KeyguardViewMediator" : "com.android.keyguard.KeyguardViewMediator",
                    lPParam.classLoader, "onScreenTurnedOff", int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            prefsApps.reload();
                            if (prefsApps.getBoolean(IMOD_RESET_ON_SCREEN_OFF, false)) {
                                clear();
                                log("ML: Screen turned off, locked apps.");
                            }
                        }
                    });
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void clear() throws Throwable {
        FileWriter fw = new FileWriter(Main.TEMPS_PATH);
        fw.write("{\"TheAnswer\":\"42\"}");
        fw.close();
    }
}
