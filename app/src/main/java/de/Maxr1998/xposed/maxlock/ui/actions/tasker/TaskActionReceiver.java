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

package de.Maxr1998.xposed.maxlock.ui.actions.tasker;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionsHelper;
import de.Maxr1998.xposed.maxlock.ui.actions.BundleScrubber;

public class TaskActionReceiver extends BroadcastReceiver {

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Common.TASKER_ENABLED, false) ||
                !intent.getAction().equals("com.twofortyfouram.locale.intent.action.FIRE_SETTING")
                || BundleScrubber.scrub(intent)) {
            return;
        }
        final Bundle extra = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        if (BundleScrubber.scrub(extra)) {
            return;
        }
        ActionsHelper.callAction(extra.getInt(ActionsHelper.ACTION_EXTRA_KEY, -1), context);
    }
}
