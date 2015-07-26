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

import android.annotation.SuppressLint;
import android.os.Build;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedBridge.log;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    public static final String PACKAGE_NAME = "de.Maxr1998.xposed.maxlock";
    @SuppressLint("SdCardPath")
    public static final String TEMPS_PATH = "/data/data/" + PACKAGE_NAME + "/files/temps.json";
    private static XSharedPreferences prefsApps;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        log("Loaded class Main from MaxLock");
        prefsApps = new XSharedPreferences(PACKAGE_NAME, "packages");
        prefsApps.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lPParam) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && lPParam.packageName.equals(ScreenOff.PACKAGE_NAME)) {
            ScreenOff.init(prefsApps, lPParam, true);
        } else if (lPParam.packageName.equals(ScreenOff.PACKAGE_NAME_LEGACY)) {
            ScreenOff.init(prefsApps, lPParam, false);
        }

        if (lPParam.packageName.equals(MaxLock.PACKAGE_NAME)) {
            File tmp = new File(TEMPS_PATH);
            boolean created = tmp.createNewFile();
            boolean rwSuccess = tmp.setReadable(true, false) && tmp.setWritable(true, false);
            log(created ? "Temp-file created.\n" : "" + (rwSuccess ? "Permissions set." : "Error settings permissions!"));
            MaxLock.init(lPParam);
        }

        if (prefsApps.getBoolean(lPParam.packageName, false)) {
            Apps.init(prefsApps, lPParam);
        }
    }
}
