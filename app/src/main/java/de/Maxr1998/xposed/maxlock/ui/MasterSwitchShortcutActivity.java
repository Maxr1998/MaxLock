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

package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.widget.MasterSwitchWidget;

@SuppressLint("CommitPrefEdits")
public class MasterSwitchShortcutActivity extends FragmentActivity implements AuthenticationSucceededListener {

    private SharedPreferences prefsApps;

    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("LaunchOnly", false)) {
            // Launch
            Log.d("MaxLock", "Launching shortcut");
            prefsApps = MLPreferences.getPrefsApps(this);
            if (prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)) {
                setContentView(R.layout.activity_lock);
                Fragment frag = new LockFragment();
                Bundle b = new Bundle(1);
                b.putString(Common.INTENT_EXTRAS_PKG_NAME, getString(R.string.unlock_master_switch));
                frag.setArguments(b);
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();
            } else {
                prefsApps.edit().putBoolean(Common.MASTER_SWITCH_ON, true).commit();
                Toast.makeText(this, getString(R.string.toast_master_switch_on), Toast.LENGTH_LONG).show();
                fireIntentAndFinish();
            }
        } else {
            // Create shortcut
            Log.d("MaxLock", "Creating shortcut");
            Intent shortcut = new Intent(this, MasterSwitchShortcutActivity.class);
            shortcut.putExtra("LaunchOnly", true);
            Intent install = new Intent();
            install.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.toggle_master_switch));
            install.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
            install.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
            setResult(RESULT_OK, install);
            fireIntentAndFinish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        fireIntentAndFinish();
    }

    @Override
    public void onAuthenticationSucceeded() {
        prefsApps.edit().putBoolean(Common.MASTER_SWITCH_ON, false).commit();
        Toast.makeText(this, getString(R.string.toast_master_switch_off), Toast.LENGTH_LONG).show();
        fireIntentAndFinish();
    }

    private void fireIntentAndFinish() {
        Intent intent = new Intent(this, MasterSwitchWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), MasterSwitchWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
        finish();
    }
}