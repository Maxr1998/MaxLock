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

import android.os.Build;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.Maxr1998.xposed.maxlock.Common.MAXLOCK_PACKAGE_NAME;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.getDefault;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.writeFile;
import static de.robv.android.xposed.XposedBridge.log;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static XSharedPreferences prefsApps;

    static void logD(String message) {
        if (BuildConfig.DEBUG)
            log(message);
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        log("ML: Loaded Main class");
        prefsApps = new XSharedPreferences(MAXLOCK_PACKAGE_NAME, "packages");
        prefsApps.makeWorldReadable();
        writeFile(getDefault());
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lPParam) throws Throwable {
        if (lPParam.packageName.equals(MaxLock.PACKAGE_NAME)) {
            MaxLock.init(lPParam);
        } else if (lPParam.packageName.equals(DeviceAdminProtection.PACKAGE_NAME)) {
            DeviceAdminProtection.init(lPParam);
        } else if (lPParam.packageName.equals(SystemUI.PACKAGE_NAME)) {
            SystemUI.init(prefsApps, lPParam);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                SystemUI.initScreenOff(prefsApps, lPParam);
            }
            return;
        } else if (lPParam.packageName.equals(SystemUI.PACKAGE_NAME_KEYGUARD) && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            SystemUI.initScreenOff(prefsApps, lPParam);
            return;
        }
        prefsApps.reload();
        if (prefsApps.getBoolean(lPParam.packageName, false)) {
            Apps.init(prefsApps, lPParam);
        } else {
            Apps.initLogging(lPParam);
        }
    }
}