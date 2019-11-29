/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2018  Max Rumpf alias Maxr1998
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

package de.Maxr1998.xposed.maxlock.preferences;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

import de.Maxr1998.xposed.maxlock.Common;

public class SharedPreferencesProvider extends RemotePreferenceProvider {

    public SharedPreferencesProvider() {
        super(Common.PREFERENCE_PROVIDER_AUTHORITY, new String[]{Common.MAXLOCK_PACKAGE_NAME.concat("_preferences"), Common.PREFS_HISTORY, Common.PREFS_APPS});
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        return prefName.equals(Common.PREFS_HISTORY) || (!write && (prefName.equals(Common.PREFS_APPS) || prefKey.equals(Common.HIDE_RECENTS_THUMBNAILS) || prefKey.equals(Common.USE_DARK_STYLE)));
    }
}