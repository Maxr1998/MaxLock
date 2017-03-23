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

package de.Maxr1998.xposed.maxlock.ui.settings;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.preference.PreferenceFragmentCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.Toast;

import com.haibison.android.lockpattern.LockPatternActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.MLImplementation;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.KnockCodeSetupFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.PinSetupFragment;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static de.Maxr1998.xposed.maxlock.ui.SettingsActivity.isSecondPane;

public final class MaxLockPreferenceFragment extends PreferenceFragmentCompat {

    private static final int WALLPAPER_REQUEST_CODE = 42;
    private static final int BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE = 100;
    private SharedPreferences prefs;
    private Screen screen;

    private Snackbar snackCache;

    public static void launchFragment(@NonNull FragmentManager manager, @NonNull Fragment fragment, boolean fromRoot) {
        if (fromRoot) {
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        manager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_in, R.anim.fragment_out, R.anim.fragment_pop_in, R.anim.fragment_pop_out)
                .replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
        SettingsActivity.showMultipane(manager);
    }

    private void setTitle() {
        // Only apply title for main screen if back stack is empty (prevent multipane from setting title)
        if ((getFragmentManager().getBackStackEntryCount() == 0 && screen == Screen.MAIN) || screen != Screen.MAIN) {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(screen.preferenceXML);
        switch (screen) {
            case MAIN:
                setRetainInstance(true);
                // Show changelog and rating dialog
                if (BuildConfig.VERSION_CODE > prefs.getInt(Common.LAST_VERSION_NUMBER, 0)) {
                    showUpdatedMessage();
                    prefs.edit().putInt(Common.LAST_VERSION_NUMBER, BuildConfig.VERSION_CODE).apply();
                } else {
                    if (!isSecondPane(this) && allowRatingDialog()) {
                        prefs.edit().putInt(Common.RATING_DIALOG_APP_OPENING_COUNTER, 0)
                                .putLong(Common.RATING_DIALOG_LAST_SHOWN, System.currentTimeMillis()).apply();
                        @SuppressLint("InflateParams") View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_like_app, null);
                        @SuppressWarnings("ResourceType") final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.dialog_cb_never_again);
                        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (checkBox.isChecked()) {
                                    prefs.edit().putBoolean(Common.RATING_DIALOG_SHOW_NEVER, true).apply();
                                }
                                switch (i) {
                                    case -3:
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
                                        } catch (ActivityNotFoundException e) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
                                        }
                                        break;
                                    case -1:
                                        startActivity(new Intent(getActivity(), DonateActivity.class));
                                        break;
                                }
                            }
                        };
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.dialog_like_app)
                                .setView(dialogView)
                                .setPositiveButton(R.string.dialog_button_donate, onClickListener)
                                .setNeutralButton(R.string.dialog_button_rate, onClickListener)
                                .setNegativeButton(android.R.string.cancel, onClickListener).create().show();
                    }
                }
                CheckBoxPreference useDark = (CheckBoxPreference) findPreference(Common.USE_DARK_STYLE);
                if (!useDark.isChecked()) {
                    PreferenceCategory cat = (PreferenceCategory) findPreference(Common.CATEGORY_APPLICATION_UI);
                    cat.removePreference(findPreference(Common.USE_AMOLED_BLACK));
                }
                findPreference(Common.ABOUT).setTitle(getName() + " " + BuildConfig.VERSION_NAME);
                Preference pro = findPreference(Common.ENABLE_PRO);
                if (prefs.getBoolean(Common.DONATED, false)) {
                    pro.setEnabled(false);
                    pro.setSummary("");
                    if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                        prefs.edit().putBoolean(Common.ENABLE_PRO, true).apply();
                        prefs.edit().putBoolean(Common.ENABLE_LOGGING, true).apply();
                    }
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
                /*SharedPreferences prefsTheme = getActivity().getSharedPreferences(Common.PREFS_THEME, Context.MODE_WORLD_READABLE);
                Preference[] overriddenByTheme = {findPreference(Common.BACKGROUND), findPreference(Common.HIDE_TITLE_BAR), findPreference(Common.HIDE_INPUT_BAR), findPreference(Common.SHOW_KC_DIVIDER), findPreference(Common.MAKE_KC_TOUCH_VISIBLE)};
                if (prefsTheme.contains(Common.THEME_PKG)) {
                    Preference themeManager = findPreference(Common.OPEN_THEME_MANAGER);
                    themeManager.setSummary(getString(R.string.pref_open_theme_manager_summary_applied) + prefsTheme.getString(Common.THEME_PKG, ""));
                    for (Preference preference : overriddenByTheme) {
                        preference.setEnabled(false);
                        preference.setSummary(preference.getSummary() != null ? preference.getSummary() : " " + getString(R.string.pref_summary_overridden_by_theme));
                    }
                }*/
                ListPreference lp = (ListPreference) findPreference(Common.BACKGROUND);
                findPreference(Common.BACKGROUND_COLOR).setEnabled(lp.getValue().equals("color"));
                lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (preference.getKey().equals(Common.BACKGROUND)) {
                            if (newValue.toString().equals("custom")) {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("image/*");
                                startActivityForResult(intent, WALLPAPER_REQUEST_CODE);
                            } else {
                                FileUtils.deleteQuietly(new File(getActivity().getFilesDir(), "background"));
                            }
                            findPreference(Common.BACKGROUND_COLOR).setEnabled(newValue.toString().equals("color"));
                        }
                        return true;
                    }
                });
                break;
            case OPTIONS:
                Preference el = findPreference(Common.ENABLE_LOGGING);
                el.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
                if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                    el.setSummary(R.string.toast_pro_required);
                }
                break;
            case IMOD:
                //Intika I.Mod - Pro setup
                Preference iModDelayGlobal = findPreference(Common.ENABLE_IMOD_DELAY_GLOBAL);
                Preference iModDelayPerApp = findPreference(Common.ENABLE_IMOD_DELAY_APP);
                iModDelayGlobal.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
                iModDelayPerApp.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
                if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                    iModDelayGlobal.setTitle(R.string.pref_delay_needpro);
                    iModDelayPerApp.setTitle(R.string.pref_delay_needpro);
                }
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setTitle();
        if (screen == Screen.MAIN && ((SettingsActivity) getActivity()).isDeviceAdminActive()) {
            Preference protectOrUninstall = findPreference(Common.UNINSTALL);
            protectOrUninstall.setTitle(R.string.pref_uninstall);
            protectOrUninstall.setSummary("");
        }

        // Show Snackbars if no password and/or packages set up
        if (screen == Screen.MAIN && !isSecondPane(this)) {
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
                        .setAction(R.string.sb_action_setup, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                launchFragment(getFragmentManager(), copyFragment, true);
                            }
                        });
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
        //noinspection ConstantConditions
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(screen != Screen.MAIN || getFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        getListView().setPadding(0, 0, 0, 0);
        getListView().setOverscrollFooter(new ColorDrawable(v.getContext().obtainStyledAttributes(new int[]{R.attr.windowBackground}).getColor(0, ContextCompat.getColor(v.getContext(), R.color.default_window_background))));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            getListView().setSelector(v.getContext().obtainStyledAttributes(new int[]{R.attr.highlightDrawable}).getDrawable(0));
            ContextCompat.getDrawable(v.getContext(), getResources().getIdentifier("overscroll_edge", "drawable", "android")).setColorFilter(ContextCompat.getColor(v.getContext(), R.color.primary_red), PorterDuff.Mode.SRC_ATOP);
            ContextCompat.getDrawable(v.getContext(), getResources().getIdentifier("overscroll_glow", "drawable", "android")).setColorFilter(ContextCompat.getColor(v.getContext(), R.color.primary_red), PorterDuff.Mode.SRC_ATOP);
        }
    }

    private StringBuilder getName() {
        StringBuilder name = new StringBuilder(getString(R.string.app_name));
        if (!Util.isDevMode()) {
            if (prefs.getBoolean(Common.ENABLE_PRO, false)) {
                name.append(" ");
                name.append(getString(prefs.getBoolean(Common.DONATED, false) ? R.string.name_pro : R.string.name_pseudo_pro));
            }
        } else {
            name.append(" Indev");
        }
        return name;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey() == null) {
            return false;
        }
        switch (screen) {
            case MAIN:
                switch (preference.getKey()) {
                    case Common.ML_IMPLEMENTATION:
                        AlertDialog implementation = new AlertDialog.Builder(getContext())
                                .setTitle(preference.getTitle())
                                .setView(MLImplementation.createImplementationDialog(getContext()))
                                .setNegativeButton(android.R.string.ok, null)
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        ((SettingsActivity) getActivity()).updateXposedStatusAlert();
                                    }
                                })
                                .create();
                        implementation.show();
                        return true;
                    case Common.LOCKING_TYPE_SETTINGS:
                        launchFragment(getFragmentManager(), Screen.TYPE.getScreen(), true);
                        return true;
                    case Common.LOCKING_UI_SETTINGS:
                        launchFragment(getFragmentManager(), Screen.UI.getScreen(), true);
                        return true;
                    case Common.LOCKING_OPTIONS:
                        launchFragment(getFragmentManager(), Screen.OPTIONS.getScreen(), true);
                        return true;
                    case Common.IMOD_OPTIONS:
                        launchFragment(getFragmentManager(), Screen.IMOD.getScreen(), true);
                        return true;
                    case Common.CHOOSE_APPS:
                        launchFragment(getFragmentManager(), new AppListFragment(), true);
                        return true;
                    case Common.HIDE_APP_FROM_LAUNCHER:
                        TwoStatePreference hideApp = (TwoStatePreference) preference;
                        if (hideApp.isChecked()) {
                            Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                            ComponentName componentName = new ComponentName(getActivity(), "de.Maxr1998.xposed.maxlock.Main");
                            getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        } else {
                            ComponentName componentName = new ComponentName(getActivity(), "de.Maxr1998.xposed.maxlock.Main");
                            getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        }
                        return true;
                    case Common.USE_DARK_STYLE:
                    case Common.USE_AMOLED_BLACK:
                    case Common.ENABLE_PRO:
                        ((SettingsActivity) getActivity()).restart();
                        return true;
                    case Common.ABOUT:
                        launchFragment(getFragmentManager(), Screen.ABOUT.getScreen(), true);
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
                                            "API: " + Build.VERSION.SDK_INT + ", Fingerprint: " + Build.FINGERPRINT);
                            FileUtils.copyFileToDirectory(getActivity().getFileStreamPath("history.json"), tempDirectory);
                            Process process = Runtime.getRuntime().exec("logcat -d");
                            FileUtils.copyInputStreamToFile(process.getInputStream(), new File(tempDirectory, "logcat.txt"));
                            FileUtils.copyFileToDirectory(new File(getActivity().getPackageManager()
                                    .getApplicationInfo("de.robv.android.xposed.installer", 0).dataDir + "/log", "error.log"), tempDirectory);
                            // Create zip
                            File zipFile = new File(getActivity().getCacheDir(), "report.zip");
                            ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
                            Util.writeDirectoryToZip(tempDirectory, stream);
                            stream.close();
                            FileUtils.deleteQuietly(tempDirectory);
                            Util.checkForStoragePermission(this, BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE, R.string.dialog_storage_permission_bug_report);
                        } catch (IOException | PackageManager.NameNotFoundException e) {
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
                        launchFragment(getFragmentManager(), new PinSetupFragment(), false);
                        return true;
                    case Common.LOCKING_TYPE_KNOCK_CODE:
                        launchFragment(getFragmentManager(), new KnockCodeSetupFragment(), false);
                        return true;
                    case Common.LOCKING_TYPE_PATTERN:
                        Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, getActivity(), LockPatternActivity.class);
                        startActivityForResult(intent, Util.getPatternCode(-1));
                        return true;
                }
                break;
            case OPTIONS:
                switch (preference.getKey()) {
                    case Common.VIEW_LOGS:
                        launchFragment(getFragmentManager(), new LogViewerFragment(), false);
                        return true;
                }
                break;
            case ABOUT:
                switch (preference.getKey()) {
                    case Common.SHOW_CHANGELOG:
                        showChangelog();
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
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    File zipFile = new File(getActivity().getCacheDir(), "report.zip");
                    File external = new File(Common.EXTERNAL_FILES_DIR, zipFile.getName());
                    FileUtils.deleteQuietly(external);

                    // Move files and send email
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.dev_email)});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "MaxLock feedback on " + Build.MODEL);
                    intent.putExtra(Intent.EXTRA_TEXT, "Please here describe your issue as DETAILED as possible!");
                    try {
                        FileUtils.moveFile(zipFile, external);
                        FileUtils.deleteQuietly(zipFile);
                        Uri uri = Uri.fromFile(external);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.dialog_message_bugreport_finished_select_email)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(Intent.createChooser(intent, getString(R.string.share_menu_title_send_email)));
                                }
                            }).create().show();
                }
                break;
        }
    }

    private boolean allowRatingDialog() {
        return !prefs.getBoolean(Common.RATING_DIALOG_SHOW_NEVER, false) && (prefs.getInt(Common.RATING_DIALOG_APP_OPENING_COUNTER, 0) >= 25 ||
                System.currentTimeMillis() - prefs.getLong(Common.RATING_DIALOG_LAST_SHOWN, System.currentTimeMillis()) > TimeUnit.DAYS.toMillis(14));
    }

    private void showUpdatedMessage() {
        AlertDialog message = new AlertDialog.Builder(getActivity())
                .setMessage(R.string.dialog_maxlock_updated)
                .setNegativeButton(R.string.dialog_button_whats_new, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showChangelog();
                    }
                })
                .setPositiveButton(R.string.dialog_button_got_it, null)
                .create();
        message.setCanceledOnTouchOutside(false);
        message.show();
    }

    private void showChangelog() {
        AlertDialog.Builder changelog = new AlertDialog.Builder(getActivity());
        WebView wv = new WebView(getContext());
        wv.setWebViewClient(new WebViewClient());
        wv.getSettings().setUserAgentString("MaxLock App v" + BuildConfig.VERSION_NAME);
        wv.loadUrl("http://maxlock.maxr1998.de/files/changelog-base.php");
        changelog.setView(wv);
        changelog.setPositiveButton(android.R.string.ok, null);
        changelog.create().show();
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