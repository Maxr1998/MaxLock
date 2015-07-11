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

import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.hooks.Main.MY_PACKAGE_NAME;
import static de.Maxr1998.xposed.maxlock.hooks.Main.clear;
import static de.Maxr1998.xposed.maxlock.hooks.Main.put;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class Receiver implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lPParam) throws Throwable {
        if (!lPParam.packageName.equals(MY_PACKAGE_NAME)) {
            return;
        }

        findAndHookMethod(MY_PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "openApp", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                put(XposedHelpers.getObjectField(param.thisObject, "packageName") + Common.FLAG_TMP);
            }
        });
        findAndHookMethod(MY_PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onAuthenticationSucceeded", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                put(XposedHelpers.getObjectField(param.thisObject, "packageName") + Common.FLAG_IMOD, Common.IMOD_LAST_UNLOCK_GLOBAL);
            }
        });
        findAndHookMethod(MY_PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onBackPressed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                put(XposedHelpers.getObjectField(param.thisObject, "packageName") + Common.FLAG_CLOSE_APP);
            }
        });
        findAndHookMethod(MY_PACKAGE_NAME + ".tasker.TaskActionReceiver", lPParam.classLoader, "clearImod", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                clear();
            }
        });

        XposedHelpers.setStaticBooleanField(findClass(MY_PACKAGE_NAME + ".ui.SettingsActivity", lPParam.classLoader), "IS_ACTIVE", true);
    }
}