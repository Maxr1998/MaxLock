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

package de.Maxr1998.xposed.maxlock.ui.actions.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionActivity;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionsHelper;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;

public class MasterSwitchWidget extends AppWidgetProvider {

    @SuppressLint("WorldReadableFiles")
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, ActionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ActionsHelper.ACTION_EXTRA_KEY, ActionsHelper.ACTION_TOGGLE_MASTER_SWITCH);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        boolean masterSwitchOn = MLPreferences.getPrefsApps(context).getBoolean(Common.MASTER_SWITCH_ON, true);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.master_switch_widget);
            views.setImageViewResource(R.id.widget_master_switch_icon, masterSwitchOn ? R.drawable.ic_lock_48dp : R.drawable.ic_lock_open_48dp);
            views.setOnClickPendingIntent(R.id.widget_background, pending);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}