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

import android.content.pm.ApplicationInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListHolder {

    private static ListHolder sInstance;
    private final List<ApplicationInfo> mAppList = new ArrayList<>();
    private final List<ApplicationInfo> mAppListBackup = new ArrayList<>();

    private ListHolder() {
    }

    public static ListHolder getInstance() {
        if (sInstance == null) {
            sInstance = new ListHolder();
        }
        return sInstance;
    }

    public void setItems(Collection<ApplicationInfo> collection) {
        mAppList.clear();
        if (collection != null)
            mAppList.addAll(collection);
    }

    public ApplicationInfo get(int location) {
        return mAppList.get(location);
    }

    public int size() {
        return mAppList.size();
    }

    public List<ApplicationInfo> backup() {
        if (mAppListBackup.isEmpty()) {
            mAppListBackup.addAll(mAppList);
        }
        return mAppListBackup;
    }

    public void restore() {
        if (!mAppListBackup.isEmpty()) {
            setItems(mAppListBackup);
            mAppListBackup.clear();
        }
    }

    public void wipe() {
        mAppListBackup.clear();
        mAppList.clear();
    }

    public boolean initialized() {
        return !mAppList.isEmpty() || !mAppListBackup.isEmpty();
    }
}
