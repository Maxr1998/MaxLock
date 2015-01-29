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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.CompoundButton;

import com.google.android.gms.analytics.GoogleAnalytics;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.lib.StatusBarTintApi;
import de.Maxr1998.xposed.maxlock.ui.settings.SettingsFragment;


public class SettingsActivity extends ActionBarActivity implements AuthenticationSucceededListener {

    private static final String TAG_SETTINGS_FRAGMENT = "tag_settings_fragment";
    public static boolean IS_DUAL_PANE;
    private static boolean UNLOCKED = false;
    public SettingsFragment mSettingsFragment;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Util.cleanUp(this);

        // Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_locking_type, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_locking_ui, false);
        if (prefs.getBoolean(Common.USE_DARK_STYLE, false)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSettingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(TAG_SETTINGS_FRAGMENT);
        if (mSettingsFragment == null) {
            getSupportActionBar().hide();
            if (getSupportFragmentManager().findFragmentById(R.id.settings_fragment) != null)
                getSupportFragmentManager().beginTransaction().hide(getSupportFragmentManager().findFragmentById(R.id.settings_fragment)).commit();
            Fragment lockFragment = new LockFragment();
            Bundle b = new Bundle(1);
            b.putString(Common.INTENT_EXTRAS_PKG_NAME, getApplicationContext().getPackageName());
            lockFragment.setArguments(b);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, lockFragment).commit();
        }
        ((ThisApplication) getApplication()).getTracker(ThisApplication.TrackerName.APP_TRACKER);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        SwitchCompat master_switch = (SwitchCompat) MenuItemCompat.getActionView(menu.findItem(R.id.master_switch_menu_item));
        master_switch.setChecked(getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE).getBoolean(Common.MASTER_SWITCH_ON, true));
        master_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint({"CommitPrefEdits"})
            @Override
            public void onCheckedChanged(CompoundButton button, boolean b) {
                getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE).edit().putBoolean(Common.MASTER_SWITCH_ON, b).commit();
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            if (getSupportFragmentManager().findFragmentById(R.id.settings_fragment) != null && getSupportFragmentManager().getBackStackEntryCount() == 1)
                getSupportFragmentManager().beginTransaction().hide(getSupportFragmentManager().findFragmentById(R.id.settings_fragment)).commit();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        UNLOCKED = true;
        if (mSettingsFragment == null) {
            mSettingsFragment = new SettingsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, mSettingsFragment, TAG_SETTINGS_FRAGMENT).commit();
            getSupportActionBar().show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.ENABLE_PRO, false) &&
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.ENABLE_LOGGING, false) && !UNLOCKED) {
            Util.logFailedAuthentication(this, "Main App");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatusBarTintApi.sendColorChangeIntent(getResources().getColor(R.color.primary_red_dark), -3, getResources().getColor(android.R.color.black), -3, this);
    }

    public void restart() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.restart_required);
        builder.setTitle(R.string.app_name)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = getIntent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        finish();
                        startActivity(intent);
                    }
                }).create().show();
    }
}