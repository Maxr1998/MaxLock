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
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import java.util.Arrays;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.lib.StatusBarTintApi;
import de.Maxr1998.xposed.maxlock.ui.firstStart.FirstStartActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.MaxLockPreferenceFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.Startup;
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static de.Maxr1998.xposed.maxlock.util.Util.LOG_TAG_ADMIN;

public class SettingsActivity extends AppCompatActivity implements AuthenticationSucceededListener {

    private static final String TAG_SETTINGS_FRAGMENT = "SettingsFragment";
    private static final String TAG_LOCK_FRAGMENT = "LockFragment";
    private static final Uri WEBSITE_URI = Uri.parse("http://maxlock.maxr1998.de/?client=inapp&lang=" + Util.getLanguageCode());
    @SuppressWarnings({"FieldCanBeLocal", "CanBeFinal"})
    private static boolean IS_ACTIVE = false;
    private static boolean UNLOCKED = false;
    public ComponentName deviceAdmin;
    private MaxLockPreferenceFragment mSettingsFragment;
    private DevicePolicyManager devicePolicyManager;
    private CustomTabsServiceConnection mConnection;
    private CustomTabsSession mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getInt(FirstStartActivity.FIRST_START_LAST_VERSION_KEY, 0) != FirstStartActivity.FIRST_START_LATEST_VERSION) {
            startActivity(new Intent(this, FirstStartActivity.class));
            finish();
            return;
        }

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(this, UninstallProtectionReceiver.class);

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (!IS_ACTIVE) {
            View maxlockActive = findViewById(R.id.xposed_active);
            maxlockActive.setVisibility(View.VISIBLE);
            maxlockActive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog help = new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(R.string.maxlock_inactive)
                            .setMessage(R.string.dialog_message_not_active)
                            .create();
                    help.show();
                }
            });
        }

        mSettingsFragment = (MaxLockPreferenceFragment) getSupportFragmentManager().findFragmentByTag(TAG_SETTINGS_FRAGMENT);
        if (mSettingsFragment == null) {
            new Startup(this).execute(prefs.getBoolean(Common.FIRST_START, true));
            // Hide Action bar and fragment
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            if (getSupportFragmentManager().findFragmentById(R.id.multi_pane_settings_fragment) != null) {
                getSupportFragmentManager().beginTransaction().hide(getSupportFragmentManager().findFragmentById(R.id.multi_pane_settings_fragment)).commit();
            }
            // Show lockscreen
            Fragment lockFragment = new LockFragment();
            Bundle b = new Bundle(1);
            b.putStringArray(Common.INTENT_EXTRAS_NAMES, new String[]{getApplicationContext().getPackageName(), getClass().getName()});
            lockFragment.setArguments(b);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, lockFragment, TAG_LOCK_FRAGMENT).commit();
        }

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                customTabsClient.warmup(0);
                mSession = customTabsClient.newSession(new CustomTabsCallback());
                if (mSession == null) {
                    return;
                }
                Bundle maxr1998Website = new Bundle();
                maxr1998Website.putParcelable(CustomTabsService.KEY_URL, Uri.parse("http://maxr1998.de/"));
                Bundle technosparksProfile = new Bundle();
                technosparksProfile.putParcelable(CustomTabsService.KEY_URL, Uri.parse("http://greenwap.nfshost.com/about/shahmi"));
                mSession.mayLaunchUrl(WEBSITE_URI, null, Arrays.asList(technosparksProfile, maxr1998Website));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        CustomTabsClient.bindCustomTabsService(this, "com.android.chrome", mConnection);
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
        Fragment lockScreen = getSupportFragmentManager().findFragmentByTag(TAG_LOCK_FRAGMENT);
        if (getSupportActionBar() != null && !getSupportActionBar().isShowing() && (lockScreen == null || !lockScreen.isVisible())) {
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
                @SuppressWarnings("deprecation") CustomTabsIntent intent = new CustomTabsIntent.Builder(mSession)
                        .setShowTitle(true)
                        .enableUrlBarHiding()
                        .setToolbarColor(getResources().getColor(R.color.primary_red))
                        .build();
                intent.launchUrl(this, WEBSITE_URI);
                return true;
            case android.R.id.home:
                onBackPressed();
                return false;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                //noinspection ConstantConditions
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                if (getSupportFragmentManager().findFragmentById(R.id.multi_pane_settings_fragment) != null) {
                    getSupportFragmentManager().beginTransaction().hide(getSupportFragmentManager().findFragmentById(R.id.multi_pane_settings_fragment)).commit();
                }
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        UNLOCKED = true;
        if (mSettingsFragment == null) {
            mSettingsFragment = MaxLockPreferenceFragment.Screen.MAIN.getScreen();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.fragment_in, R.anim.fragment_out);
            ft.replace(R.id.frame_container, mSettingsFragment, TAG_SETTINGS_FRAGMENT).commit();
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
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            StatusBarTintApi.sendColorChangeIntent(ContextCompat.getColor(this, R.color.primary_red_dark), -3, Color.BLACK, -3, this);
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
        if (mConnection != null) {
            unbindService(mConnection);
        }
        super.onDestroy();
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

    public CustomTabsSession getSession() {
        return mSession;
    }

    public DevicePolicyManager getDevicePolicyManager() {
        return devicePolicyManager;
    }

    public boolean isDeviceAdminActive() {
        return devicePolicyManager.isAdminActive(deviceAdmin);
    }

    public static class UninstallProtectionReceiver extends DeviceAdminReceiver {

        @Override
        public void onEnabled(Context context, Intent intent) {
            super.onEnabled(context, intent);
            Log.i(LOG_TAG_ADMIN, "Device admin is now active!");
        }
    }
}