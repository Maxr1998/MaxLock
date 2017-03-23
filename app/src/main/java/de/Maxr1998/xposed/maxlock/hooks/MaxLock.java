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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.Common.MAXLOCK_PACKAGE_NAME;
import static de.Maxr1998.xposed.maxlock.hooks.Main.logD;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.getDefault;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.writeFile;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

class MaxLock {

    static final String PACKAGE_NAME = MAXLOCK_PACKAGE_NAME;

    static void init(final XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod(PACKAGE_NAME + ".MLImplementation", lPParam.classLoader, "isXposedActive", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
            findAndHookMethod(PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onAuthenticationSucceeded", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String[] names = (String[]) getObjectField(param.thisObject, "names");
                    logD("ML|Unlocked " + names[1]);
                }
            });
            findAndHookMethod(PACKAGE_NAME + ".ui.actions.ActionsHelper", lPParam.classLoader, "clearImod", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    writeFile(getDefault());
                    return null;
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }
}