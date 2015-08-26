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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionActivity;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionsHelper;

public abstract class NotificationHelper {

    public static final int NOTIFICATION_ID = 0x130D;

    public static void postNotification(Context mContext) {
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!MLPreferences.getPrefsApps(mContext).getBoolean(Common.IMOD_SHOW_RESET_NOTIFICATION, false)) {
            nm.cancel(NOTIFICATION_ID);
            return;
        }
        Intent notifyIntent = new Intent(mContext, ActionActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notifyIntent.putExtra(ActionsHelper.ACTION_EXTRA_KEY, R.id.radio_imod_reset);

        Notification.Builder builder = new Notification.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.action_imod_reset))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_close_white_24dp)
                .setContentIntent(PendingIntent.getActivity(mContext.getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            builder.setShowWhen(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            builder.setPriority(Notification.PRIORITY_MIN)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setColor(mContext.getResources().getColor(R.color.accent));
        }
        //noinspection deprecation
        nm.notify(NOTIFICATION_ID, builder.getNotification());
    }
}
