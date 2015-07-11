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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import com.anjlab.android.iab.v3.BillingProcessor;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.lib.StatusBarTintApi;
import de.Maxr1998.xposed.maxlock.ui.FirstStart.FirstStartActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.SettingsFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.Startup;
import de.Maxr1998.xposed.maxlock.ui.settings.WebsiteFragment;
import de.Maxr1998.xposed.maxlock.util.BillingHelper;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

public class SettingsActivity extends AppCompatActivity implements AuthenticationSucceededListener {

    private static final String TAG_SETTINGS_FRAGMENT = "SettingsFragment";
    private static final String TAG_WEBSITE_FRAGMENT = "WebsiteFragment";
    private static final String TAG_LOCK_FRAGMENT = "LockFragment";
    public static boolean IS_ACTIVE = false;
    static boolean FS_SHOW = true;
    private static boolean UNLOCKED = false;
    public SettingsFragment mSettingsFragment;
    SharedPreferences prefs;
    private BillingProcessor billingProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(Common.USE_DARK_STYLE, false)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        if ((FS_SHOW && Util.isDevMode()) || prefs.getInt(FirstStartActivity.FIRST_START_LAST_VERSION_KEY, 0) != FirstStartActivity.FIRST_START_LATEST_VERSION) {
            startActivity(new Intent(this, FirstStartActivity.class));
            FS_SHOW = false;
            finish();
            return;
        }

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (!IS_ACTIVE) {
            findViewById(R.id.xposed_active).setVisibility(View.VISIBLE);
        }

        mSettingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(TAG_SETTINGS_FRAGMENT);
        if (mSettingsFragment == null) {
            new Startup(this).execute(prefs.getBoolean(Common.FIRST_START, true));
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            if (getSupportFragmentManager().findFragmentById(R.id.settings_fragment) != null) {
                getSupportFragmentManager().beginTransaction().hide(getSupportFragmentManager().findFragmentById(R.id.settings_fragment)).commit();
            }
            Fragment lockFragment = new LockFragment();
            Bundle b = new Bundle(1);
            b.putString(Common.INTENT_EXTRAS_PKG_NAME, getApplicationContext().getPackageName());
            lockFragment.setArguments(b);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, lockFragment, TAG_LOCK_FRAGMENT).commit();
        }
        billingProcessor = new BillingProcessor(this, getString(R.string.license_key), mSettingsFragment);
        if (BillingHelper.GooglePlayServiceAvailable(getApplicationContext())) {
            billingProcessor.loadOwnedPurchasesFromGoogle();
        }
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        SwitchCompat master_switch = (SwitchCompat) MenuItemCompat.getActionView(menu.findItem(R.id.toolbar_master_switch));
        //noinspection deprecation
        master_switch.setChecked(getSharedPreferences(Common.PREFS_APPS, Context.MODE_WORLD_READABLE).getBoolean(Common.MASTER_SWITCH_ON, true));
        master_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint({"CommitPrefEdits"})
            @Override
            public void onCheckedChanged(CompoundButton button, boolean b) {
                MLPreferences.getPrefsApps(SettingsActivity.this).edit().putBoolean(Common.MASTER_SWITCH_ON, b).commit();
            }
        });
        Fragment appsList = getSupportFragmentManager().findFragmentByTag("AppListFragment");
        if (appsList != null && appsList.isVisible()) {
            menu.findItem(R.id.toolbar_info).setVisible(false);
            menu.findItem(R.id.toolbar_master_switch).setVisible(false);
        }
        Fragment website = getSupportFragmentManager().findFragmentByTag(TAG_WEBSITE_FRAGMENT);
        Fragment lockScreen = getSupportFragmentManager().findFragmentByTag(TAG_LOCK_FRAGMENT);
        if (website != null && website.isVisible() && getSupportActionBar() != null && getSupportFragmentManager().findFragmentById(R.id.settings_fragment) == null) {
            getSupportActionBar().hide();
        } else if (getSupportActionBar() != null && !getSupportActionBar().isShowing() && (lockScreen == null || !lockScreen.isVisible())) {
            getSupportActionBar().show();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SwitchCompat master_switch = (SwitchCompat) MenuItemCompat.getActionView(menu.findItem(R.id.toolbar_master_switch));
        master_switch.setChecked(MLPreferences.getPrefsApps(SettingsActivity.this).getBoolean(Common.MASTER_SWITCH_ON, true));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_info:
                SettingsFragment.launchFragment(new WebsiteFragment(), true, mSettingsFragment);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment website = getSupportFragmentManager().findFragmentByTag(TAG_WEBSITE_FRAGMENT);
        if (website != null && website.isVisible()) {
            if (((WebsiteFragment) website).back())
                return;
        }
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
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            StatusBarTintApi.sendColorChangeIntent(getResources().getColor(R.color.primary_red_dark), -3, getResources().getColor(android.R.color.black), -3, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.ENABLE_LOGGING, false) && !UNLOCKED) {
            Util.logFailedAuthentication(this, "Main App");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingProcessor != null) billingProcessor.release();
    }

    public BillingProcessor getBillingProcessor() {
        return billingProcessor;
    }

    public void restart() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_text_restart_required);
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