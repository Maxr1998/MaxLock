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

package de.Maxr1998.xposed.maxlock.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.MasterSwitchShortcutActivity;

public class MasterSwitchWidget extends AppWidgetProvider {

    @SuppressLint("WorldReadableFiles")
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, MasterSwitchShortcutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("LaunchOnly", true);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        //noinspection deprecation
        SharedPreferences prefsPackages = context.getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE);
        boolean masterSwitchOn = prefsPackages.getBoolean(Common.MASTER_SWITCH_ON, true);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.master_switch_widget);
            views.setImageViewResource(R.id.widget_master_switch_icon, masterSwitchOn ? R.drawable.ic_widget_on_72dp : R.drawable.ic_widget_off_72dp);
            views.setOnClickPendingIntent(R.id.widget, pending);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
