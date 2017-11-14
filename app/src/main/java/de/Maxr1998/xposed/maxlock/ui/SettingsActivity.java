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

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.Arrays;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
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

    public static final String TAG_PREFERENCE_FRAGMENT_SECOND_PANE = "SecondPanePreferenceFragment";
    private static final String TAG_PREFERENCE_FRAGMENT = "MLPreferenceFragment";
    private static boolean UNLOCKED = false;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public ComponentName deviceAdmin;
    private Toolbar toolbar;
    private LockView lockscreen;
    private FrameLayout secondFragmentContainer;
    private DevicePolicyManager devicePolicyManager;
    private CustomTabsServiceConnection mConnection;
    private CustomTabsSession mSession;

    public static void showMultipaneIfInLandscape(SettingsActivity activity) {
        if (activity.secondFragmentContainer != null) {
            activity.secondFragmentContainer.setVisibility(View.VISIBLE);
            FragmentManager manager = activity.getSupportFragmentManager();
            Fragment secondPane = manager.findFragmentByTag(TAG_PREFERENCE_FRAGMENT_SECOND_PANE);
            if (secondPane == null) {
                secondPane = MaxLockPreferenceFragment.Screen.MAIN.getScreen();
            }
            if (!secondPane.isAdded())
                manager.beginTransaction().replace(R.id.second_fragment_container, secondPane, TAG_PREFERENCE_FRAGMENT_SECOND_PANE).commit();
        }
    }

    public boolean inLandscape() {
        return secondFragmentContainer != null;
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
        ViewGroup contentView = findViewById(R.id.content_view_settings);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        secondFragmentContainer = findViewById(R.id.second_fragment_container);

        Fragment settingsFragment = getSupportFragmentManager().findFragmentByTag(TAG_PREFERENCE_FRAGMENT);
        if (settingsFragment == null) {
            // Main fragment doesn't exist, app just opened
            // → Show lockscreen
            UNLOCKED = false;
            lockscreen = new LockView(this, null);
            contentView.addView(lockscreen, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            // → Create and display settings
            settingsFragment = getIntent().getAction() != null && getIntent().getAction().equals(BuildConfig.APPLICATION_ID + ".VIEW_APPS") ?
                    new AppListFragment() : MaxLockPreferenceFragment.Screen.MAIN.getScreen();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settingsFragment, TAG_PREFERENCE_FRAGMENT).commit();

            // → Hide Action bar
            toolbar.setTranslationY(-getResources().getDimensionPixelSize(R.dimen.toolbar_height));
            // Run startup
            new Startup(this).execute();
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
                mSession.mayLaunchUrl(Common.WEBSITE_URI, null, Arrays.asList(maxr1998Website, technoSparksProfile));
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
        SwitchCompat master_switch = (SwitchCompat) menu.findItem(R.id.toolbar_master_switch).getActionView();
        master_switch.setChecked(MLPreferences.getPrefsApps(this).getBoolean(Common.MASTER_SWITCH_ON, true));
        master_switch.setOnCheckedChangeListener((button, b) -> MLPreferences.getPrefsApps(SettingsActivity.this).edit().putBoolean(Common.MASTER_SWITCH_ON, b).apply());
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SwitchCompat master_switch = (SwitchCompat) menu.findItem(R.id.toolbar_master_switch).getActionView();
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
            if (getSupportFragmentManager().getBackStackEntryCount() == 1 && inLandscape())
                secondFragmentContainer.setVisibility(View.GONE);
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        UNLOCKED = true;
        lockscreen.removeAllViews();
        ObjectAnimator animator = ObjectAnimator.ofFloat(toolbar, "translationY", 0f);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(200);
        animator.start();
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
        if (!UNLOCKED && MLPreferences.getPreferences(this).getBoolean(Common.ENABLE_LOGGING, false)) {
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
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    Intent intent = getIntent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    finish();
                    startActivity(intent);
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