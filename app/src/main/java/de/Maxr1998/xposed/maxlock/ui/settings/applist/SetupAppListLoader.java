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

package de.Maxr1998.xposed.maxlock.ui.settings.applist;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.AsyncTaskLoader;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.util.KUtil;

class SetupAppListLoader extends AsyncTaskLoader<List<ApplicationInfo>> {

    public static boolean LOAD_ALL = false;

    public SetupAppListLoader(Context c) {
        super(c);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public List<ApplicationInfo> loadInBackground() {
        if (ListHolder.getInstance().initialized()) {
            return null;
        }
        final PackageManager pm = getContext().getPackageManager();
        final List<ApplicationInfo> mAllApps = pm.getInstalledApplications(0);
        final List<ApplicationInfo> result = new ArrayList<>();
        final List<String> launcherPackages = KUtil.getLauncherPackages(pm);
        for (int i = 0; i < mAllApps.size(); i++) {
            ApplicationInfo ai = mAllApps.get(i);
            if ((shouldLoad(pm, ai) && !ai.packageName.equals(BuildConfig.APPLICATION_ID)) && !launcherPackages.contains(ai.packageName) || ai.packageName.matches("com.(google.)?android.packageinstaller")) {
                result.add(ai);
            }
        }
        Collections.sort(result, new Comparator<ApplicationInfo>() {
            Collator sCollator = Collator.getInstance();

            @Override
            public int compare(ApplicationInfo one, ApplicationInfo two) {
                return sCollator.compare(one.loadLabel(pm).toString(), two.loadLabel(pm).toString());
            }
        });
        return result;
    }

    private boolean shouldLoad(PackageManager pm, ApplicationInfo ai) {
        try {
            if (LOAD_ALL && pm.getPackageInfo(ai.packageName, PackageManager.GET_ACTIVITIES).activities != null) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pm.getLaunchIntentForPackage(ai.packageName) != null;
    }
}