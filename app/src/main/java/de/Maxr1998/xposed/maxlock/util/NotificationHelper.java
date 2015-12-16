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

package de.Maxr1998.xposed.maxlock.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionActivity;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionsHelper;

public final class NotificationHelper {

    public static final int NOTIFICATION_ID = 0x130D;

    public static void postNotification(Context mContext) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(mContext);
        if (!MLPreferences.getPrefsApps(mContext).getBoolean(Common.SHOW_IMOD_RESET_NOTIFICATION, false)) {
            nm.cancel(NOTIFICATION_ID);
            return;
        }
        Intent notifyIntent = new Intent(mContext, ActionActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notifyIntent.putExtra(ActionsHelper.ACTION_EXTRA_KEY, R.id.radio_imod_reset);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.action_imod_reset))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_apps_24dp)
                .setContentIntent(PendingIntent.getActivity(mContext.getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setOngoing(true)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            builder.setShowWhen(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setColor(ContextCompat.getColor(mContext, R.color.accent));
        }
        nm.notify(NOTIFICATION_ID, builder.build());
    }
}
