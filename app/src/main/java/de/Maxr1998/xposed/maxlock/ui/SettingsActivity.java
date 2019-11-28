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

package de.Maxr1998.xposed.maxlock.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import java.util.Arrays;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.firstStart.FirstStartActivity;
import de.Maxr1998.xposed.maxlock.ui.lockscreen.LockView;
import de.Maxr1998.xposed.maxlock.ui.settings.MaxLockPreferenceFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListFragment;
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static de.Maxr1998.xposed.maxlock.util.Util.LOG_TAG_ADMIN;

public class SettingsActivity extends AppCompatActivity implements AuthenticationSucceededListener {

    public static final String TAG_PREFERENCE_FRAGMENT = "MLPreferenceFragment";
    public static final String TAG_PREFERENCE_FRAGMENT_SECOND_PANE = "SecondPanePreferenceFragment";
    private static boolean UNLOCKED = false;

    public ComponentName deviceAdmin;
    private SettingsViewModel settingsViewModel;
    private Toolbar toolbar;
    private ViewGroup contentView;
    private LockView lockscreen;
    private FrameLayout secondFragmentContainer;
    private DevicePolicyManager devicePolicyManager;
    private CustomTabsServiceConnection ctConnection;
    private CustomTabsSession ctSession;

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

        settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(this, UninstallProtectionReceiver.class);

        setContentView(R.layout.activity_settings);
        contentView = findViewById(R.id.content_view_settings);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        secondFragmentContainer = findViewById(R.id.second_fragment_container);

        Fragment settingsFragment = getSupportFragmentManager().findFragmentByTag(TAG_PREFERENCE_FRAGMENT);
        if (settingsFragment == null || !UNLOCKED) {
            // Main fragment doesn't exist, app just opened
            // → Show lockscreen
            UNLOCKED = false;
            if (!MLPreferences.getPreferences(this).getString(Common.LOCKING_TYPE, "").isEmpty()) {
                contentView.addView(lockscreen = new LockView(this, null), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                lockscreen.forceFocus();

                // → Hide Action bar
                toolbar.setTranslationY(-getResources().getDimensionPixelSize(R.dimen.toolbar_height));
            } else UNLOCKED = true;
            // → Create and display settings
            settingsFragment = getIntent().getAction() != null && getIntent().getAction().equals(BuildConfig.APPLICATION_ID + ".VIEW_APPS") ?
                    new AppListFragment() : MaxLockPreferenceFragment.Screen.MAIN.getScreen();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settingsFragment, TAG_PREFERENCE_FRAGMENT).commit();
        }

        ctConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                customTabsClient.warmup(0);
                ctSession = customTabsClient.newSession(new CustomTabsCallback());
                if (ctSession == null)
                    return;
                Bundle maxr1998Website = new Bundle();
                maxr1998Website.putParcelable(CustomTabsService.KEY_URL, Common.MAXR1998_URI);
                Bundle technoSparksProfile = new Bundle();
                technoSparksProfile.putParcelable(CustomTabsService.KEY_URL, Common.TECHNO_SPARKS_URI);
                ctSession.mayLaunchUrl(Common.WEBSITE_URI, null, Arrays.asList(maxr1998Website, technoSparksProfile));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        String cctPackageName = CustomTabsClient.getPackageName(this, Arrays.asList(
                "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "org.mozilla.firefox", "org.mozilla.firefox_beta"
        ));
        if (cctPackageName != null) {
            CustomTabsClient.bindCustomTabsService(this, cctPackageName, ctConnection);
        } else ctConnection = null;
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        Switch master_switch = (Switch) menu.findItem(R.id.toolbar_master_switch).getActionView();
        master_switch.setChecked(MLPreferences.getPrefsApps(this).getBoolean(Common.MASTER_SWITCH_ON, true));
        master_switch.setOnCheckedChangeListener((button, b) -> MLPreferences.getPrefsApps(SettingsActivity.this).edit().putBoolean(Common.MASTER_SWITCH_ON, b).apply());
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Switch master_switch = (Switch) menu.findItem(R.id.toolbar_master_switch).getActionView();
        master_switch.setChecked(MLPreferences.getPrefsApps(SettingsActivity.this).getBoolean(Common.MASTER_SWITCH_ON, true));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_info:
                @SuppressWarnings("deprecation") CustomTabsIntent intent = new CustomTabsIntent.Builder(ctSession)
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
        contentView.removeView(lockscreen);
        ObjectAnimator animator = ObjectAnimator.ofFloat(toolbar, "translationY", 0f).setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
        Fragment settingsFragment = getSupportFragmentManager().findFragmentByTag(TAG_PREFERENCE_FRAGMENT);
        if (settingsFragment instanceof MaxLockPreferenceFragment)
            contentView.postDelayed(((MaxLockPreferenceFragment) settingsFragment)::onLockscreenDismissed, 250);
    }

    @Override
    protected void onStart() {
        super.onStart();
        invalidateOptionsMenu();
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
        if (ctConnection != null)
            unbindService(ctConnection);
        super.onDestroy();
    }

    public void restart() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_text_restart_required);
        builder.setTitle(R.string.app_name)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    Intent intent = new Intent(this, getClass());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    finish();
                    startActivity(intent);
                }).create().show();
    }

    public CustomTabsSession getSession() {
        return ctSession;
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