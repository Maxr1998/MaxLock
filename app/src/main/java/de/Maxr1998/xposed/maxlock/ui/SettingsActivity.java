/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2016 Max Rumpf alias Maxr1998
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
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import java.util.Arrays;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.MLImplementation;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.lib.StatusBarTintApi;
import de.Maxr1998.xposed.maxlock.ui.firstStart.FirstStartActivity;
import de.Maxr1998.xposed.maxlock.ui.lockscreen.LockView;
import de.Maxr1998.xposed.maxlock.ui.settings.MaxLockPreferenceFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.Startup;
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListFragment;
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static de.Maxr1998.xposed.maxlock.util.Util.LOG_TAG_ADMIN;

public class SettingsActivity extends AppCompatActivity implements AuthenticationSucceededListener {

    private static final String TAG_PREFERENCE_FRAGMENT = "MLPreferenceFragment";
    private static final String TAG_PREFERENCE_FRAGMENT_SECOND_PANE = "SecondPanePreferenceFragment";
    private static final String TAG_LOCK_FRAGMENT = "LockFragment";
    private static boolean UNLOCKED = false;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public ComponentName deviceAdmin;
    private Fragment mSettingsFragment;
    private DevicePolicyManager devicePolicyManager;
    private CustomTabsServiceConnection mConnection;
    private CustomTabsSession mSession;

    public static boolean isSecondPane(Fragment f) {
        return f.getTag() != null && f.getTag().equals(TAG_PREFERENCE_FRAGMENT_SECOND_PANE);
    }

    /**
     * Show second pane if available
     */
    public static void showMultipane(FragmentManager manager) {
        Fragment secondPane = manager.findFragmentByTag(TAG_PREFERENCE_FRAGMENT_SECOND_PANE);
        if (secondPane != null) {
            manager.beginTransaction().show(secondPane).commit();
        }
    }

    /**
     * Hide second pane if visible
     */
    public static void hideMultipane(FragmentManager manager) {
        Fragment secondPane = manager.findFragmentByTag(TAG_PREFERENCE_FRAGMENT_SECOND_PANE);
        if (secondPane != null) {
            manager.beginTransaction().hide(secondPane).commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        if (MLPreferences.getPreferences(this).getInt(FirstStartActivity.FIRST_START_LAST_VERSION_KEY, 0) != FirstStartActivity.FIRST_START_LATEST_VERSION) {
            startActivity(new Intent(this, FirstStartActivity.class));
        }

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(this, UninstallProtectionReceiver.class);

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Hide multipane view
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            hideMultipane(getSupportFragmentManager());
        }
        mSettingsFragment = getSupportFragmentManager().findFragmentByTag(TAG_PREFERENCE_FRAGMENT);
        if (mSettingsFragment == null) {
            // Main fragment not visible → app just opened
            if (getSupportFragmentManager().findFragmentByTag(TAG_LOCK_FRAGMENT) == null) {
                // Lockscreen not visible as well → run startup & show lockscreen
                new Startup(this).execute();
                UNLOCKED = false;
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Lockscreen(), TAG_LOCK_FRAGMENT).commit();
            }
            // Hide Action bar
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
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
                maxr1998Website.putParcelable(CustomTabsService.KEY_URL, Common.MAXR1998_URI);
                Bundle technoSparksProfile = new Bundle();
                technoSparksProfile.putParcelable(CustomTabsService.KEY_URL, Common.TECHNO_SPARKS_URI);
                Bundle knownProblemSettings = new Bundle();
                knownProblemSettings.putParcelable(CustomTabsService.KEY_URL, Common.KNOWN_PROBLEM_SETTINGS_URI);
                mSession.mayLaunchUrl(Common.WEBSITE_URI, null, Arrays.asList(technoSparksProfile, maxr1998Website, knownProblemSettings));
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
        master_switch.setChecked(MLPreferences.getPrefsApps(this).getBoolean(Common.MASTER_SWITCH_ON, true));
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
                intent.launchUrl(this, Common.WEBSITE_URI);
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
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                hideMultipane(getSupportFragmentManager());
            }
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        UNLOCKED = true;
        if (mSettingsFragment == null) {
            mSettingsFragment = getIntent().getAction().equals(BuildConfig.APPLICATION_ID + ".VIEW_APPS") ?
                    new AppListFragment() : MaxLockPreferenceFragment.Screen.MAIN.getScreen();
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_in, R.anim.fragment_out)
                    .replace(R.id.fragment_container, mSettingsFragment, TAG_PREFERENCE_FRAGMENT)
                    .commit();
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
        }
        updateXposedStatusAlert();
    }

    public void updateXposedStatusAlert() {
        //noinspection ConstantConditions
        if (!MLImplementation.isXposedActive()) {
            boolean showWarning = MLImplementation.getImplementation(MLPreferences.getPreferences(this)) == MLImplementation.DEFAULT;
            //noinspection ConstantConditions
            findViewById(R.id.xposed_active).setVisibility(showWarning ? View.VISIBLE : View.GONE);
            //noinspection ConstantConditions
            findViewById(R.id.xposed_active_message).setOnClickListener(new View.OnClickListener() {
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
            Util.logFailedAuthentication(this, getPackageName());
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

    @SuppressWarnings("WeakerAccess")
    public static class Lockscreen extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return new LockView(LockView.getThemedContext(getActivity()), getActivity().getApplicationContext().getPackageName(), SettingsActivity.class.getName());
        }
    }
}