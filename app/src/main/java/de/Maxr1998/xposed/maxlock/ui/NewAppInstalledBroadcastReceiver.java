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

package de.Maxr1998.xposed.maxlock.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.NotificationHelper;

import static de.Maxr1998.xposed.maxlock.util.NotificationHelper.APP_INSTALLED_NOTIFICATION_ID;

public class NewAppInstalledBroadcastReceiver extends BroadcastReceiver {

    public static final String MAXLOCK_ACTION_NEVER_SHOW_AGAIN = "ml_nsa";
    public static final String MAXLOCK_ACTION_LOCK_APP = "ml_la";
    public static final String EXTRA_PACKAGE_NAME = "package_name";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        switch (intent.getAction()) {
            case Intent.ACTION_PACKAGE_ADDED:
                if (!MLPreferences.getPreferences(context).getBoolean(Common.NEW_APP_NOTIFICATION, true) ||
                        intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) ||
                        intent.getData() == null || intent.getData().getSchemeSpecificPart().length() == 0) {
                    return;
                }
                String newPackageName = intent.getData().getSchemeSpecificPart();
                if (context.getPackageManager().getLaunchIntentForPackage(newPackageName) != null) {
                    NotificationHelper.postAppInstalledNotification(context, newPackageName);
                }
                break;
            case MAXLOCK_ACTION_LOCK_APP:
                String lockPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
                if (lockPackageName != null) {
                    MLPreferences.getPrefsApps(context).edit().putBoolean(lockPackageName, true).apply();
                    Toast.makeText(context, R.string.toast_lock_new_app_locked_successfully, Toast.LENGTH_SHORT).show();
                }
                NotificationManagerCompat.from(context).cancel(APP_INSTALLED_NOTIFICATION_ID);
                break;
            case MAXLOCK_ACTION_NEVER_SHOW_AGAIN:
                MLPreferences.getPreferences(context).edit().putBoolean(Common.NEW_APP_NOTIFICATION, false).apply();
                NotificationManagerCompat.from(context).cancel(APP_INSTALLED_NOTIFICATION_ID);
                break;
            case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                if (intent.getData() != null && intent.getData().getSchemeSpecificPart().length() > 0) {
                    MLPreferences.getPrefsApps(context).edit().remove(intent.getData().getSchemeSpecificPart()).apply();
                }
                break;
        }
    }
}