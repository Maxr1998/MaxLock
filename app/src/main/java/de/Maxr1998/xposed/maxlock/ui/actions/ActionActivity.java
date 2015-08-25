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

package de.Maxr1998.xposed.maxlock.ui.actions;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.LockFragment;
import de.Maxr1998.xposed.maxlock.ui.actions.widget.MasterSwitchWidget;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;

public class ActionActivity extends AppCompatActivity implements AuthenticationSucceededListener {

    private int mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefsApps = MLPreferences.getPrefsApps(this);
        mode = getIntent().getIntExtra(ActionsHelper.ACTION_EXTRA_KEY, -1);
        if (mode == R.id.radio_ms_off || (mode == R.id.radio_toggle_ms && prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true))) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_lock);
            Fragment frag = new LockFragment();
            Bundle b = new Bundle(1);
            b.putString(Common.INTENT_EXTRAS_PKG_NAME, getString(R.string.unlock_master_switch));
            frag.setArguments(b);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();
        } else {
            ActionsHelper.callAction(mode, this);
            fireIntentAndFinish();
            super.onCreate(savedInstanceState);
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        ActionsHelper.callAction(mode, this);
        fireIntentAndFinish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fireIntentAndFinish();
    }

    private void fireIntentAndFinish() {
        // Update MasterSwitch Widget if existent
        if (mode == R.id.radio_toggle_ms || mode == R.id.radio_ms_on || mode == R.id.radio_ms_off) {
            Intent intent = new Intent(this, MasterSwitchWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), MasterSwitchWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        }
        finish();
    }
}
