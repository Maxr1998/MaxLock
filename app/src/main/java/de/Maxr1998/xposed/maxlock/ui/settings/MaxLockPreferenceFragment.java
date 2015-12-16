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

package de.Maxr1998.xposed.maxlock.ui.settings;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragmentCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.haibison.android.lockpattern.LockPatternActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.DonateActivity;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.KnockCodeSetupFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.PinSetupFragment;
import de.Maxr1998.xposed.maxlock.util.Util;

public class MaxLockPreferenceFragment extends PreferenceFragmentCompat {

    private static final int WALLPAPER_REQUEST_CODE = 42;
    private static final int BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE = 100;
    private SharedPreferences prefs;
    private Screen screen;

    public static void launchFragment(@NonNull Fragment fragment, boolean fromRoot, @NonNull Fragment from) {
        if (fromRoot) {
            from.getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        FragmentTransaction ft = from.getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fragment_in, R.anim.fragment_out, R.anim.fragment_pop_in, R.anim.fragment_pop_out);
        ft.replace(R.id.frame_container, fragment, fragment instanceof AppListFragment ? "AppListFragment" : null).addToBackStack(null).commit();
        if (from.getFragmentManager().findFragmentById(R.id.multi_pane_settings_fragment) != null)
            from.getFragmentManager().beginTransaction().show(from.getFragmentManager().findFragmentById(R.id.multi_pane_settings_fragment)).commit();
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
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setTitle();
        if (screen == Screen.IMOD) {
            getPreferenceManager().setSharedPreferencesName(Common.PREFS_APPS);
        }
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(screen.preferenceXML);
        switch (screen) {
            case MAIN:
                setRetainInstance(true);
                findPreference(Common.ABOUT).setTitle(getName() + " " + BuildConfig.VERSION_NAME);
                Preference pro = findPreference(Common.ENABLE_PRO);
                if (prefs.getBoolean(Common.DONATED, false)) {
                    pro.setEnabled(false);
                    pro.setSummary("");
                    prefs.edit().putBoolean(Common.ENABLE_PRO, true).apply();
                }
                TwoStatePreference useDark = (TwoStatePreference) findPreference(Common.USE_DARK_STYLE);
                if (useDark.isChecked()) {
                    CheckBoxPreference amoledBlack = new CheckBoxPreference(useDark.getContext());
                    amoledBlack.setKey(Common.USE_AMOLED_BLACK);
                    amoledBlack.setTitle(R.string.pref_use_amoled_black);
                    amoledBlack.setSummary(R.string.pref_use_amoled_black_summary);
                    amoledBlack.setOrder(useDark.getOrder() + 1);
                    ((PreferenceCategory) findPreference(Common.CATEGORY_APPLICATION_UI)).addPreference(amoledBlack);
                    amoledBlack.setDependency(Common.USE_DARK_STYLE);
                }
                break;
            case TYPE:
                break;
            case UI:
                SharedPreferences prefsTheme = getActivity().getSharedPreferences(Common.PREFS_THEME, Context.MODE_WORLD_READABLE);
                Preference[] overriddenByTheme = {findPreference(Common.BACKGROUND), findPreference(Common.HIDE_TITLE_BAR), findPreference(Common.HIDE_INPUT_BAR), findPreference(Common.SHOW_KC_DIVIDER), findPreference(Common.MAKE_KC_TOUCH_VISIBLE)};
                if (prefsTheme.contains(Common.THEME_PKG)) {
                    Preference themeManager = findPreference(Common.OPEN_THEME_MANAGER);
                    themeManager.setSummary(getString(R.string.pref_open_theme_manager_summary_applied) + prefsTheme.getString(Common.THEME_PKG, ""));
                    for (Preference preference : overriddenByTheme) {
                        preference.setEnabled(false);
                        preference.setSummary(preference.getSummary() != null ? preference.getSummary() : " " + getString(R.string.pref_summary_overridden_by_theme));
                    }
                }
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
                            }
                            findPreference(Common.BACKGROUND_COLOR).setEnabled(newValue.toString().equals("color"));
                        }
                        return true;
                    }
                });
                Preference tabletMode = findPreference(Common.OVERRIDE_TABLET_MODE);
                tabletMode.setSummary(String.format(getString(R.string.pref_use_tablet_mode_summary),
                        Build.MODEL, getResources().getBoolean(R.bool.tablet_mode_default) ? "tablet/phablet" : "phone",
                        (int) getResources().getDisplayMetrics().xdpi, Math.min(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels)));
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setTitle();
        if (screen != Screen.MAIN) {
            //noinspection ConstantConditions
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setTitle() {
        if (screen == Screen.MAIN) {
            getActivity().setTitle(getName());
        } else {
            getActivity().setTitle(screen.title);
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
                    case Common.LOCKING_TYPE_SETTINGS:
                        launchFragment(Screen.TYPE.getScreen(), true, this);
                        return true;
                    case Common.LOCKING_UI_SETTINGS:
                        launchFragment(Screen.UI.getScreen(), true, this);
                        return true;
                    case Common.LOCKING_OPTIONS:
                        prefs.edit().putBoolean(Common.ENABLE_LOGGING, prefs.getBoolean(Common.ENABLE_PRO, false)).apply();
                        launchFragment(Screen.OPTIONS.getScreen(), true, this);
                        return true;
                    case Common.IMOD_OPTIONS:
                        launchFragment(Screen.IMOD.getScreen(), true, this);
                        return true;
                    case Common.CHOOSE_APPS:
                        launchFragment(new AppListFragment(), true, this);
                        return true;
                    case Common.HIDE_APP_FROM_LAUNCHER:
                        TwoStatePreference hideApp = (TwoStatePreference) preference;
                        if (hideApp.isChecked()) {
                            Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                            ComponentName componentName = new ComponentName(getActivity(), "de.Maxr1998.xposed.maxlock.ui.SettingsActivity");
                            getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        } else {
                            ComponentName componentName = new ComponentName(getActivity(), "de.Maxr1998.xposed.maxlock.ui.SettingsActivity");
                            getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        }
                        return true;
                    case Common.USE_DARK_STYLE:
                    case Common.USE_AMOLED_BLACK:
                    case Common.ENABLE_PRO:
                        ((SettingsActivity) getActivity()).restart();
                        return true;
                    case Common.ABOUT:
                        launchFragment(Screen.ABOUT.getScreen(), true, this);
                        return true;
                    case Common.DONATE:
                        getActivity().startActivity(new Intent(getActivity(), DonateActivity.class));
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
                            getActivity().startActivity(uninstall);
                        }
                        return true;
                    case Common.SEND_FEEDBACK:
                        File tempDirectory = new File(getActivity().getCacheDir(), "feedback-cache");
                        try {
                            // Obtain data
                            FileUtils.copyDirectoryToDirectory(new File(Util.dataDir(getActivity()), "shared_prefs"), tempDirectory);
                            FileUtils.writeStringToFile(new File(tempDirectory, "device-info.txt"), Build.MANUFACTURER + " " + Build.MODEL + ", " +
                                    "SDK" + Build.VERSION.SDK_INT + ", Fingerprint " + Build.FINGERPRINT);
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
                        launchFragment(new PinSetupFragment(), false, this);
                        return true;
                    case Common.LOCKING_TYPE_KNOCK_CODE:
                        launchFragment(new KnockCodeSetupFragment(), false, this);
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
                        launchFragment(new LogViewerFragment(), false, this);
                        return true;
                }
                break;
            case ABOUT:
                switch (preference.getKey()) {
                    case Common.VISIT_WEBSITE:
                        CustomTabsIntent devWebsite = new CustomTabsIntent.Builder(((SettingsActivity) getActivity()).getSession())
                                .setShowTitle(true)
                                .enableUrlBarHiding()
                                .setToolbarColor(Color.parseColor("#ffc107"))
                                .build();
                        devWebsite.launchUrl(getActivity(), Uri.parse("http://maxr1998.de/"));
                        return true;
                    case "technosparks_profile":
                        CustomTabsIntent greenwapWebsite = new CustomTabsIntent.Builder(((SettingsActivity) getActivity()).getSession())
                                .setShowTitle(true)
                                .enableUrlBarHiding()
                                .setToolbarColor(Color.parseColor("#6d993f"))
                                .build();
                        greenwapWebsite.launchUrl(getActivity(), Uri.parse("http://greenwap.nfshost.com/about/shahmi"));
                        return true;
                }
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getListView().setPadding(0, 0, 0, 0);
        TypedValue windowBackground = new TypedValue();
        getListView().getContext().getTheme().resolveAttribute(R.attr.windowBackground, windowBackground, true);
        getListView().setOverscrollFooter(new ColorDrawable(windowBackground.data));
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
                    intent.putExtra(Intent.EXTRA_SUBJECT, "MaxLock feedback/bug-report");
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

    public enum Screen {
        MAIN(R.string.app_name, R.xml.preferences_main),
        TYPE(R.string.pref_screen_locking_type, R.xml.preferences_locking_type),
        UI(R.string.pref_screen_locking_ui, R.xml.preferences_locking_ui),
        OPTIONS(R.string.pref_screen_locking_options, R.xml.preferences_locking_options),
        IMOD(R.string.pref_screen_locking_intika, R.xml.preferences_locking_imod),
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
