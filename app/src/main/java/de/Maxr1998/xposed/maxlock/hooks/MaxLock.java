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

import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.hooks.Apps.CLOSE_OBJECT_KEY;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.IMOD_LAST_UNLOCK_GLOBAL;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.IMOD_OBJECT_KEY;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.addToHistory;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.getDefault;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.readFile;
import static de.Maxr1998.xposed.maxlock.hooks.Apps.writeFile;
import static de.Maxr1998.xposed.maxlock.hooks.Main.MAXLOCK_PACKAGE_NAME;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class MaxLock {

    public static final String PACKAGE_NAME = MAXLOCK_PACKAGE_NAME;

    public static void init(final XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            XposedHelpers.setStaticBooleanField(findClass(PACKAGE_NAME + ".ui.SettingsActivity", lPParam.classLoader), "IS_ACTIVE", true);
            findAndHookMethod(PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onAuthenticationSucceeded", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    JSONObject history = readFile();
                    addToHistory(Apps.UNLOCK_ID, lPParam.packageName, history);
                    history.put(IMOD_LAST_UNLOCK_GLOBAL, System.currentTimeMillis());
                    history.optJSONObject(IMOD_OBJECT_KEY).put(((String[]) getObjectField(param.thisObject, "names"))[0] + Apps.FLAG_IMOD, System.currentTimeMillis());
                    writeFile(history);
                }
            });
            findAndHookMethod(PACKAGE_NAME + ".ui.LockActivity", lPParam.classLoader, "onBackPressed", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    JSONObject history = readFile();
                    history.optJSONObject(CLOSE_OBJECT_KEY).put(((String[]) getObjectField(param.thisObject, "names"))[0] + Apps.FLAG_CLOSE_APP, System.currentTimeMillis());
                    writeFile(history);
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