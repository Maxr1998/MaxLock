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
    public static final String MAXLOCK_PACKAGE_NAME = "de.Maxr1998.xposed.maxlock";
    @SuppressLint("SdCardPath")
    public static final String TEMPS_PATH = "/data/data/" + MAXLOCK_PACKAGE_NAME + "/files/temps.json";
    private static XSharedPreferences prefsApps;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        log("ML: Loaded Main class");
        prefsApps = new XSharedPreferences(MAXLOCK_PACKAGE_NAME, "packages");
        prefsApps.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lPParam) throws Throwable {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && lPParam.packageName.equals(ScreenOff.PACKAGE_NAME))
                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && lPParam.packageName.equals(ScreenOff.PACKAGE_NAME_LEGACY))) {
            ScreenOff.init(prefsApps, lPParam);
            return;
        }

        if (lPParam.packageName.equals(MaxLock.PACKAGE_NAME)) {
            File tmpF = new File(TEMPS_PATH);
            File historyF = new File(Apps.HISTORY_PATH);
            boolean tCreated = tmpF.getParentFile().mkdirs() || tmpF.createNewFile();
            boolean hCreated = historyF.getParentFile().mkdirs() || historyF.createNewFile();
            boolean rwSuccess = tmpF.setReadable(true, false) && tmpF.setWritable(true, false) && historyF.setReadable(true, false) && historyF.setWritable(true, false);
            if (tCreated) log("ML: Temp-file created.");
            if (hCreated) log("ML: History created.");
            log(rwSuccess ? "ML: Permissions set." : "ML: Error settings permissions!");
            MaxLock.init(lPParam);
            return;
        }

        if (prefsApps.getBoolean(lPParam.packageName, false)) {
            Apps.init(prefsApps, lPParam);
        } else {
            Apps.initLogging(lPParam);
        }
    }
}
