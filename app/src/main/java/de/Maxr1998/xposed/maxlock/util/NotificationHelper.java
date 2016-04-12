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

package de.Maxr1998.xposed.maxlock.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.NewAppInstalledBroadcastReceiver;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionActivity;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionsHelper;

import static de.Maxr1998.xposed.maxlock.ui.NewAppInstalledBroadcastReceiver.EXTRA_PACKAGE_NAME;
import static de.Maxr1998.xposed.maxlock.ui.NewAppInstalledBroadcastReceiver.MAXLOCK_ACTION_LOCK_APP;
import static de.Maxr1998.xposed.maxlock.ui.NewAppInstalledBroadcastReceiver.MAXLOCK_ACTION_NEVER_SHOW_AGAIN;

public final class NotificationHelper {

    public static final int IMOD_NOTIFICATION_ID = 0x130D;
    public static final int APP_INSTALLED_NOTIFICATION_ID = 0x2EE;

    public static void postIModNotification(Context context) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (!MLPreferences.getPrefsApps(context).getBoolean(Common.SHOW_IMOD_RESET_NOTIFICATION, false)) {
            nm.cancel(IMOD_NOTIFICATION_ID);
            return;
        }
        Intent notifyIntent = new Intent(context, ActionActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notifyIntent.putExtra(ActionsHelper.ACTION_EXTRA_KEY, ActionsHelper.ACTION_IMOD_RESET);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.action_imod_reset))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_apps_24dp)
                .setContentIntent(PendingIntent.getActivity(context.getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setOngoing(true)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            builder.setShowWhen(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setColor(ContextCompat.getColor(context, R.color.accent));
        }
        nm.notify(IMOD_NOTIFICATION_ID, builder.build());
    }

    public static void postAppInstalledNotification(Context context, String packageName) {
        Intent neverShowAgain = new Intent(MAXLOCK_ACTION_NEVER_SHOW_AGAIN);
        neverShowAgain.setClass(context, NewAppInstalledBroadcastReceiver.class);
        Intent lockApp = new Intent(MAXLOCK_ACTION_LOCK_APP);
        lockApp.setClass(context, NewAppInstalledBroadcastReceiver.class);
        lockApp.putExtra(EXTRA_PACKAGE_NAME, packageName);

        String appName;
        try {
            appName = context.getPackageManager().getApplicationInfo(packageName, 0).loadLabel(context.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            appName = packageName;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.notification_lock_new_app_title))
                .setContentText(appName)
                .setSmallIcon(R.drawable.ic_lock_48dp)
                .setAutoCancel(true)
                .addAction(new NotificationCompat.Action(0, context.getString(R.string.notification_lock_new_app_action_never_again),
                        PendingIntent.getBroadcast(context, 0, neverShowAgain, PendingIntent.FLAG_UPDATE_CURRENT)))
                .addAction(new NotificationCompat.Action(0, context.getString(R.string.notification_lock_new_app_action_lock),
                        PendingIntent.getBroadcast(context, 0, lockApp, PendingIntent.FLAG_UPDATE_CURRENT)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                    .setColor(ContextCompat.getColor(context, R.color.accent));
            try {
                builder.setVibrate(new long[0]);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(APP_INSTALLED_NOTIFICATION_ID, builder.build());
    }
}
