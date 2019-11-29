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

package de.Maxr1998.xposed.maxlock.ui.settings;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.preference.PreferenceFragmentCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.snackbar.Snackbar;
import com.haibison.android.lockpattern.LockPatternActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipOutputStream;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.MLImplementation;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.preference.ImplementationPreference;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListFragment;
import de.Maxr1998.xposed.maxlock.ui.settings_new.SettingsUtils;
import de.Maxr1998.xposed.maxlock.util.KUtil;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static androidx.core.content.FileProvider.getUriForFile;

public final class MaxLockPreferenceFragment extends PreferenceFragmentCompat {

    private static final int WALLPAPER_REQUEST_CODE = 42;
    private static final int BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE = 100;
    private SharedPreferences prefs;
    private Screen screen;

    private Snackbar snackCache;

    private boolean intentValid = true;

    @VisibleForTesting
    public MaxLockPreferenceFragment() {
    }

    public static void launchFragment(@NonNull Fragment fragment, @NonNull Fragment replacement, boolean fromRoot) {
        FragmentManager manager = fragment.getFragmentManager();
        assert manager != null;
        if (fromRoot) {
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        FragmentTransaction transaction = manager.beginTransaction();
        if (manager.getBackStackEntryCount() > 0 || !((SettingsActivity) fragment.getActivity()).inLandscape())
            transaction.setCustomAnimations(R.anim.fragment_in, 0, R.anim.fragment_pop_in, R.anim.fragment_pop_out);
        transaction.replace(R.id.fragment_container, replacement).addToBackStack(null).commit();
        if (fromRoot && ((MaxLockPreferenceFragment) fragment).isFirstPane())
            SettingsActivity.showMultipaneIfInLandscape((SettingsActivity) fragment.getActivity());
    }

    private boolean isFirstPane() {
        return SettingsActivity.TAG_PREFERENCE_FRAGMENT.equals(getTag());
    }

    private void setTitle() {
        // Only apply title for main screen (prevent multipane from setting title)
        if (isFirstPane()) {
            if (screen == Screen.MAIN) {
                getActivity().setTitle(getName());
            } else {
                getActivity().setTitle(screen.title);
            }
        }
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        if (getArguments() != null) {
            screen = Screen.valueOf(getArguments().getString(Screen.KEY, Screen.MAIN.toString()));
        } else {
            screen = Screen.MAIN;
        }
        prefs = MLPreferences.getPreferences(getActivity());
        setTitle();
        if (screen == Screen.IMOD) {
            getPreferenceManager().setSharedPreferencesName(Common.PREFS_APPS);
        }
        addPreferencesFromResource(screen.preferenceXML);
        switch (screen) {
            case MAIN:
                updateImplementationStatus();
                PreferenceCategory catAppUI = (PreferenceCategory) findPreference(Common.CATEGORY_APPLICATION_UI);
                CheckBoxPreference useDark = (CheckBoxPreference) findPreference(Common.USE_DARK_STYLE);
                if (!useDark.isChecked()) {
                    catAppUI.removePreference(findPreference(Common.USE_AMOLED_BLACK));
                }
                if (SDK_INT >= Build.VERSION_CODES.O) {
                    catAppUI.removePreference(findPreference(Common.NEW_APP_NOTIFICATION));
                }
                break;
            case TYPE:
                FingerprintManagerCompat fpm = FingerprintManagerCompat.from(getActivity());
                if (!fpm.isHardwareDetected()) {
                    getPreferenceScreen().removePreference(findPreference(Common.SHADOW_FINGERPRINT));
                    getPreferenceScreen().removePreference(findPreference(Common.CATEGORY_FINGERPRINT));
                } else {
                    CheckBoxPreference disableFP = (CheckBoxPreference) findPreference(Common.DISABLE_FINGERPRINT);
                    if (!fpm.hasEnrolledFingerprints() && !disableFP.isChecked()) {
                        disableFP.setSummary(disableFP.getSummary() + getResources().getString(R.string.pref_fingerprint_summary_non_enrolled));
                    }
                }
                break;
            case UI:
                ListPreference lp = (ListPreference) findPreference(Common.BACKGROUND);
                findPreference(Common.BACKGROUND_COLOR).setEnabled(lp.getValue().equals("color"));
                lp.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (preference.getKey().equals(Common.BACKGROUND)) {
                        if (newValue.toString().equals("custom")) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent, WALLPAPER_REQUEST_CODE);
                        } else
                            FileUtils.deleteQuietly(new File(getActivity().getFilesDir(), "background"));
                        findPreference(Common.BACKGROUND_COLOR).setEnabled(newValue.toString().equals("color"));
                    }
                    return true;
                });
                break;
            case OPTIONS:
                Preference el = findPreference(Common.ENABLE_LOGGING);
                el.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
                if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                    el.setSummary(R.string.toast_pro_required);
                }
                if (MLImplementation.getImplementation(prefs) != MLImplementation.DEFAULT) {
                    PreferenceCategory catOther = (PreferenceCategory) findPreference(Common.CATEGORY_OTHER);
                    catOther.removePreference(findPreference(Common.HIDE_RECENTS_THUMBNAILS));
                }
                break;
            case IMOD:
                // I.Mod - Pro setup
                Preference iModDelayGlobal = findPreference(Common.ENABLE_DELAY_GENERAL);
                Preference iModDelayPerApp = findPreference(Common.ENABLE_DELAY_PER_APP);
                iModDelayGlobal.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
                iModDelayPerApp.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
                if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                    iModDelayGlobal.setTitle(R.string.pref_delay_needpro);
                    iModDelayPerApp.setTitle(R.string.pref_delay_needpro);
                }
                break;
        }
    }

    public void onLockscreenDismissed() {
        // Show changelog and rating dialog
        int lastVersionNumber = prefs.getInt(Common.LAST_VERSION_NUMBER, -1);
        if (BuildConfig.VERSION_CODE > lastVersionNumber) {
            // Don't show updated dialog on first start
            if (lastVersionNumber > 0)
                SettingsUtils.showUpdatedMessage(getActivity());
            prefs.edit().putInt(Common.LAST_VERSION_NUMBER, BuildConfig.VERSION_CODE).apply();
        }
        if (SDK_INT > Build.VERSION_CODES.O && ContextCompat.checkSelfPermission(getContext(), READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_need_storage_permission_oreo)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            ActivityCompat.requestPermissions(getActivity(), new String[]{READ_EXTERNAL_STORAGE}, 0))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setTitle();
        if (screen == Screen.MAIN) {
            findPreference(Common.ABOUT).setTitle(getName().append(" ").append(BuildConfig.VERSION_NAME));
            if (prefs.getBoolean(Common.DONATED, false)) {
                Preference donate = findPreference(Common.DONATE);
                donate.setTitle(R.string.pref_donate_thanks_for_donation);
                donate.setSummary(R.string.pref_donate_again_on_pro_summary);
                Preference pro = findPreference(Common.ENABLE_PRO);
                pro.setEnabled(false);
                pro.setSummary("");
                if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                    prefs.edit()
                            .putBoolean(Common.ENABLE_PRO, true)
                            .putBoolean(Common.ENABLE_LOGGING, true).apply();
                }
            }
            if (((SettingsActivity) getActivity()).isDeviceAdminActive()) {
                Preference protectOrUninstall = findPreference(Common.UNINSTALL);
                protectOrUninstall.setTitle(R.string.pref_uninstall);
                protectOrUninstall.setSummary("");
            }
        }

        // Show Snackbars if no password and/or packages set up
        if (screen == Screen.MAIN && isFirstPane()) {
            @StringRes int stringId = 0;
            Fragment fragment = null;
            if (prefs.getString(Common.LOCKING_TYPE, "").equals("")) {
                stringId = R.string.sb_no_locking_type;
                fragment = Screen.TYPE.getScreen();
            } else if (!new File(Util.dataDir(getContext()) + "shared_prefs" + File.separator + Common.PREFS_APPS + ".xml").exists()) {
                stringId = R.string.sb_no_locked_apps;
                fragment = new AppListFragment();
            }
            if (stringId != 0 && fragment != null) {
                final Fragment copyFragment = fragment;
                snackCache = Snackbar.make(getActivity().findViewById(android.R.id.content), stringId, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.sb_action_setup, v -> launchFragment(this, copyFragment, true));
                snackCache.show();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (screen == Screen.MAIN && snackCache != null) {
            snackCache.dismiss();
            snackCache = null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setTitle();
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            SettingsActivity.showMultipaneIfInLandscape((SettingsActivity) getActivity());
        }
        //noinspection ConstantConditions
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(screen != Screen.MAIN || getFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        getListView().setPadding(0, 0, 0, 0);
    }

    private StringBuilder getName() {
        StringBuilder name = new StringBuilder(getString(R.string.app_name));
        if (Util.isDevMode()) {
            name.append(" Indev");
        } else if (prefs.getBoolean(Common.ENABLE_PRO, false)) {
            name.append(" ").append(getString(prefs.getBoolean(Common.DONATED, false) ? R.string.name_pro : R.string.name_pseudo_pro));
        }
        return name;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey() == null)
            return false;
        switch (screen) {
            case MAIN:
                switch (preference.getKey()) {
                    case Common.ML_IMPLEMENTATION:
                        AlertDialog implementation = new AlertDialog.Builder(getContext())
                                .setTitle(preference.getTitle())
                                .setView(View.inflate(getContext(), R.layout.dialog_implementation, null))
                                .setNegativeButton(android.R.string.ok, null)
                                .setOnDismissListener(dialog -> updateImplementationStatus())
                                .create();
                        implementation.show();
                        return true;
                    case Common.LOCKING_TYPE_SETTINGS:
                        launchFragment(this, Screen.TYPE.getScreen(), true);
                        return true;
                    case Common.LOCKING_UI_SETTINGS:
                        launchFragment(this, Screen.UI.getScreen(), true);
                        return true;
                    case Common.LOCKING_OPTIONS:
                        launchFragment(this, Screen.OPTIONS.getScreen(), true);
                        return true;
                    case Common.IMOD_OPTIONS:
                        launchFragment(this, Screen.IMOD.getScreen(), true);
                        return true;
                    case Common.CHOOSE_APPS:
                        launchFragment(this, new AppListFragment(), true);
                        return true;
                    case Common.HIDE_APP_FROM_LAUNCHER:
                        TwoStatePreference hideApp = (TwoStatePreference) preference;
                        if (hideApp.isChecked()) {
                            Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                            ComponentName componentName = new ComponentName(getActivity(), "de.Maxr1998.xposed.maxlock.Main");
                            getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                            intentValid = false;
                        } else {
                            ComponentName componentName = new ComponentName(getActivity(), "de.Maxr1998.xposed.maxlock.Main");
                            getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        }
                        return true;
                    case Common.USE_DARK_STYLE:
                    case Common.USE_AMOLED_BLACK:
                    case Common.ENABLE_PRO:
                        if (intentValid) getActivity().recreate();
                        else ((SettingsActivity) getActivity()).restart();
                        return true;
                    case Common.ABOUT:
                        launchFragment(this, Screen.ABOUT.getScreen(), true);
                        return true;
                    case Common.DONATE:
                        startActivity(new Intent(getActivity(), DonateActivity.class));
                        return true;
                    case Common.UNINSTALL:
                        if (!((SettingsActivity) getActivity()).isDeviceAdminActive()) {
                            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ((SettingsActivity) getActivity()).deviceAdmin);
                            startActivity(intent);
                        } else {
                            ((SettingsActivity) getActivity()).getDevicePolicyManager().removeActiveAdmin(((SettingsActivity) getActivity()).deviceAdmin);
                            preference.setTitle(R.string.pref_prevent_uninstall);
                            preference.setSummary(R.string.pref_prevent_uninstall_summary);
                            Intent uninstall = new Intent(Intent.ACTION_DELETE);
                            uninstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            uninstall.setData(Uri.parse("package:de.Maxr1998.xposed.maxlock"));
                            startActivity(uninstall);
                        }
                        return true;
                    case Common.SEND_FEEDBACK:
                        File tempDirectory = new File(getActivity().getCacheDir(), "feedback-cache");
                        try {
                            // Obtain data
                            FileUtils.copyDirectoryToDirectory(new File(Util.dataDir(getActivity()), "shared_prefs"), tempDirectory);
                            FileUtils.writeStringToFile(new File(tempDirectory, "device-info.txt"),
                                    "App Version: " + BuildConfig.VERSION_NAME + "\n\n" +
                                            "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.PRODUCT + ")\n" +
                                            "API: " + SDK_INT + ", Fingerprint: " + Build.FINGERPRINT,
                                    Charset.forName("UTF-8"));
                            Process process = Runtime.getRuntime().exec("logcat -d");
                            FileUtils.copyInputStreamToFile(process.getInputStream(), new File(tempDirectory, "logcat.txt"));
                            // Create zip
                            File zipFile = new File(getActivity().getCacheDir() + File.separator + "export", "report.zip");
                            //noinspection ResultOfMethodCallIgnored
                            zipFile.getParentFile().mkdir();
                            FileUtils.deleteQuietly(zipFile);
                            ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
                            Util.writeDirectoryToZip(tempDirectory, stream);
                            stream.close();
                            FileUtils.deleteQuietly(tempDirectory);
                            Util.checkForStoragePermission(this, BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE, R.string.dialog_storage_permission_bug_report);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                }
                break;
            case TYPE:
                switch (preference.getKey()) {
                    case Common.LOCKING_TYPE_PASSWORD:
                        Util.setPassword(getActivity(), null);
                        return true;
                    case Common.LOCKING_TYPE_PIN:
                        LockSetupFragment lsp = new LockSetupFragment();
                        Bundle b1 = new Bundle(1);
                        b1.putString(Common.LOCKING_TYPE, Common.LOCKING_TYPE_PIN);
                        lsp.setArguments(b1);
                        launchFragment(this, lsp, false);
                        return true;
                    case Common.LOCKING_TYPE_KNOCK_CODE:
                        LockSetupFragment lsk = new LockSetupFragment();
                        Bundle b2 = new Bundle(1);
                        b2.putString(Common.LOCKING_TYPE, Common.LOCKING_TYPE_KNOCK_CODE);
                        lsk.setArguments(b2);
                        launchFragment(this, lsk, false);
                        return true;
                    case Common.LOCKING_TYPE_PATTERN:
                        Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, getActivity(), LockPatternActivity.class);
                        startActivityForResult(intent, KUtil.getPatternCode(-1));
                        return true;
                }
                break;
            case OPTIONS:
                switch (preference.getKey()) {
                    case Common.VIEW_LOGS:
                        launchFragment(this, new LogViewerFragment(), false);
                        return true;
                }
                break;
            case ABOUT:
                switch (preference.getKey()) {
                    case Common.SHOW_CHANGELOG:
                        SettingsUtils.showChangelog(getActivity());
                        return true;
                    case Common.VISIT_WEBSITE:
                        CustomTabsIntent devWebsite = new CustomTabsIntent.Builder(((SettingsActivity) getActivity()).getSession())
                                .setShowTitle(true)
                                .enableUrlBarHiding()
                                .setToolbarColor(Color.parseColor("#ffc107"))
                                .build();
                        devWebsite.launchUrl(getActivity(), Common.MAXR1998_URI);
                        return true;
                    case Common.TECHNOSPARKS_PROFILE:
                        CustomTabsIntent technosparksSite = new CustomTabsIntent.Builder(((SettingsActivity) getActivity()).getSession())
                                .setShowTitle(true)
                                .enableUrlBarHiding()
                                .setToolbarColor(Color.parseColor("#6d993f"))
                                .build();
                        technosparksSite.launchUrl(getActivity(), Common.TECHNO_SPARKS_URI);
                        return true;
                }
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (screen == Screen.TYPE && requestCode == Util.PATTERN_CODE && resultCode == LockPatternActivity.RESULT_OK) {
            Util.receiveAndSetPattern(getActivity(), data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN), null);
        } else if (screen == Screen.UI && requestCode == WALLPAPER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
            }
            if (uri == null) {
                throw new NullPointerException();
            }
            try {
                InputStream input = getActivity().getContentResolver().openInputStream(uri);
                FileOutputStream destination = getActivity().openFileOutput("background", 0);
                assert input != null;
                IOUtils.copy(input, destination);
                input.close();
                destination.close();
            } catch (IOException | AssertionError e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    File zipFile = new File(getActivity().getCacheDir() + File.separator + "export", "report.zip");

                    // Move files and send email
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.dev_email)});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "MaxLock feedback on " + Build.MODEL);
                    intent.putExtra(Intent.EXTRA_TEXT, "Please here describe your issue as DETAILED as possible!");
                    Uri uri = getUriForFile(getContext(), "de.Maxr1998.fileprovider", zipFile);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.dialog_message_bugreport_finished_select_email)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(Intent.createChooser(intent, getString(R.string.share_menu_title_send_email)))).create().show();
                }
                break;
        }
    }

    private void updateImplementationStatus() {
        ImplementationPreference implementationPreference = (ImplementationPreference) findPreference(Common.ML_IMPLEMENTATION);
        if (getContext() != null)
            implementationPreference.setWarningVisible(!MLImplementation.isActiveAndWorking(getContext(), prefs));
    }

    public enum Screen {
        MAIN(R.string.app_name, R.xml.preferences_main),
        TYPE(R.string.pref_screen_locking_type, R.xml.preferences_locking_type),
        UI(R.string.pref_screen_locking_ui, R.xml.preferences_locking_ui),
        OPTIONS(R.string.pref_screen_locking_options, R.xml.preferences_locking_options),
        IMOD(R.string.pref_screen_delayed_relock, R.xml.preferences_locking_imod),
        ABOUT(R.string.pref_screen_about, R.xml.preferences_about);

        public static String KEY = "screen";
        private int title, preferenceXML;

        Screen(@StringRes int t, @XmlRes int p) {
            title = t;
            preferenceXML = p;
        }

        public MaxLockPreferenceFragment getScreen() {
            MaxLockPreferenceFragment f = new MaxLockPreferenceFragment();
            Bundle b = new Bundle(1);
            b.putString(KEY, this.toString());
            f.setArguments(b);
            return f;
        }
    }
}