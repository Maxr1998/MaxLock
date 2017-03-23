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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.res.Resources;
import android.widget.Button;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.Common.MAXLOCK_PACKAGE_NAME;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

class DeviceAdminProtection {

    static final String PACKAGE_NAME = "com.android.settings";

    static void init(XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            findAndHookMethod(PACKAGE_NAME + ".DeviceAdminAdd", lPParam.classLoader, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ComponentName admin = (ComponentName) callMethod(getObjectField(param.thisObject, "mDeviceAdmin"), "getComponent");
                    if (((DevicePolicyManager) getObjectField(param.thisObject, "mDPM")).isAdminActive(admin) && admin.getPackageName().equals(MAXLOCK_PACKAGE_NAME)) {
                        ((Button) getObjectField(param.thisObject, "mActionButton")).setEnabled(false);
                        Resources modRes = ((Activity) param.thisObject).getPackageManager().getResourcesForApplication(MAXLOCK_PACKAGE_NAME);
                        ((TextView) getObjectField(param.thisObject, "mAdminWarning"))
                                .setText(modRes.getString(modRes.getIdentifier("maxlock_protected_admin", "string", MAXLOCK_PACKAGE_NAME),
                                        modRes.getString(modRes.getIdentifier("pref_uninstall", "string", MAXLOCK_PACKAGE_NAME))));
                    }
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }
}