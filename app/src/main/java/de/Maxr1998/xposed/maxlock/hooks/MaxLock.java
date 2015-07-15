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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.hooks.Apps.put;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class MaxLock {

    public static final String PACKAGE_NAME = "de.Maxr1998.xposed.maxlock";

    public static void init(XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod(PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "openApp", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    put(getObjectField(param.thisObject, "packageName") + Apps.FLAG_TMP);
                }
            });
            findAndHookMethod(PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onAuthenticationSucceeded", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    put(getObjectField(param.thisObject, "packageName") + Apps.FLAG_IMOD, Apps.IMOD_LAST_UNLOCK_GLOBAL);
                }
            });
            findAndHookMethod(PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onBackPressed", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    put(getObjectField(param.thisObject, "packageName") + Apps.FLAG_CLOSE_APP);
                }
            });
            findAndHookMethod(PACKAGE_NAME + ".tasker.TaskActionReceiver", lPParam.classLoader, "clearImod", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ScreenOff.clear();
                }
            });
            XposedHelpers.setStaticBooleanField(findClass(PACKAGE_NAME + ".ui.SettingsActivity", lPParam.classLoader), "IS_ACTIVE", true);
        } catch (Throwable t) {
            log(t);
        }
    }
}