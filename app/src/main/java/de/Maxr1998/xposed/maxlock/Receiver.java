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

package de.Maxr1998.xposed.maxlock;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.Common.TEMPS_FILE;
import static de.Maxr1998.xposed.maxlock.Main.MY_PACKAGE_NAME;
import static de.Maxr1998.xposed.maxlock.Main.put;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

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
                JSONObject jsonObject = new JSONObject();
                File JSONFile = new File(TEMPS_FILE);
                if (!JSONFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    JSONFile.createNewFile();
                }
                FileWriter fw = new FileWriter(JSONFile.getAbsoluteFile());
                fw.write(jsonObject.toString());
                fw.close();
            }
        });
    }
}