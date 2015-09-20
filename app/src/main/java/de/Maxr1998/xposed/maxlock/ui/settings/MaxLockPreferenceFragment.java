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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.preference.PreferenceFragmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.haibison.android.lockpattern.LockPatternActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.KnockCodeSetupFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.PinSetupFragment;
import de.Maxr1998.xposed.maxlock.util.Util;

public class MaxLockPreferenceFragment extends PreferenceFragmentCompat {

    private static final int WALLPAPER_REQUEST_CODE = 42;
    protected SharedPreferences prefs, prefsTheme;
    protected String title = null;
    private Screen screen;

    public static void launchFragment(@NonNull Fragment fragment, boolean fromRoot, @NonNull Fragment from) {
        if (fromRoot) {
            from.getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        from.getFragmentManager().beginTransaction().replace(R.id.frame_container, fragment, fragment instanceof AppListFragment ? "AppListFragment" : fragment instanceof WebsiteFragment ? "WebsiteFragment" : null).addToBackStack(null).commit();
        if (from.getFragmentManager().findFragmentById(R.id.settings_fragment) != null)
            from.getFragmentManager().beginTransaction().show(from.getFragmentManager().findFragmentById(R.id.settings_fragment)).commit();
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        screen = Screen.valueOf(getArguments().getString(Screen.KEY));
        setTitle();
        if (screen == Screen.MAIN) {
            setRetainInstance(true);
        }
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        prefsTheme = getActivity().getSharedPreferences(Common.PREFS_THEME, Context.MODE_WORLD_READABLE);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        addPreferencesFromResource(screen.preferenceXML);
        switch (screen) {
            case MAIN:
                break;
            case TYPE:
                break;
            case UI:
                Preference[] overriddenByTheme = {findPreference(Common.BACKGROUND), findPreference(Common.HIDE_TITLE_BAR), findPreference(Common.HIDE_INPUT_BAR), findPreference(Common.SHOW_KC_DIVIDERS), findPreference(Common.MAKE_KC_TOUCH_VISIBLE)};
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
                getPreferenceManager().setSharedPreferencesName(Common.PREFS_APPS);
                //Intika I.Mod - Pro setup
                Preference imod_enabled_g = findPreference(Common.ENABLE_IMOD_DELAY_GLOBAL);
                Preference imod_enabled_p = findPreference(Common.ENABLE_IMOD_DELAY_APP);
                imod_enabled_g.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
                imod_enabled_p.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
                if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                    imod_enabled_g.setTitle(R.string.pref_delay_needpro);
                    imod_enabled_p.setTitle(R.string.pref_delay_needpro);
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
        // If null, use default app name
        getActivity().setTitle(getString(screen.title != 0 && screen.title != R.string.app_name ? screen.title : R.string.app_name));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        switch (screen) {
            case MAIN:
                if (preference == findPreference(Common.LOCKING_TYPE_SETTINGS)) {
                    launchFragment(Screen.TYPE.getScreen(), true, this);
                    return true;
                } else if (preference == findPreference(Common.LOCKING_UI_SETTINGS)) {
                    launchFragment(Screen.UI.getScreen(), true, this);
                    return true;
                } else if (preference == findPreference(Common.LOCKING_OPTIONS)) {
                    prefs.edit().putBoolean(Common.ENABLE_LOGGING, prefs.getBoolean(Common.ENABLE_PRO, false)).apply();
                    launchFragment(Screen.OPTIONS.getScreen(), true, this);
                    return true;
                } else if (preference == findPreference(Common.IMOD_OPTIONS)) {
                    launchFragment(Screen.IMOD.getScreen(), true, this);
                    return true;
                } else if (preference == findPreference(Common.CHOOSE_APPS)) {
                    launchFragment(new AppListFragment(), true, this);
                    return true;
                } else if (preference == findPreference(Common.HIDE_APP_FROM_LAUNCHER)) {
                    SwitchPreference hideApp = (SwitchPreference) preference;
                    if (hideApp.isChecked()) {
                        Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                        ComponentName componentName = new ComponentName(getActivity(), "de.Maxr1998.xposed.maxlock.ui.SettingsActivity");
                        getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    } else {
                        ComponentName componentName = new ComponentName(getActivity(), "de.Maxr1998.xposed.maxlock.ui.SettingsActivity");
                        getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    }
                } else if (preference == findPreference(Common.USE_DARK_STYLE) || preference == findPreference(Common.ENABLE_PRO)) {
                    ((SettingsActivity) getActivity()).restart();
                    return true;
                } else if (preference == findPreference(Common.ABOUT)) {
                    launchFragment(Screen.ABOUT.getScreen(), true, this);
                    return true;
                } else if (preference == findPreference(Common.DONATE)) {

                    return true;
                } else if (preference == findPreference(Common.UNINSTALL)) {
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
                }
                break;
            case TYPE:
                if (preference == findPreference(Common.LOCKING_TYPE_PASSWORD)) {
                    Util.setPassword(getActivity(), null);
                    return true;
                } else if (preference == findPreference(Common.LOCKING_TYPE_PIN)) {
                    launchFragment(new PinSetupFragment(), false, this);
                    return true;
                } else if (preference == findPreference(Common.LOCKING_TYPE_KNOCK_CODE)) {
                    launchFragment(new KnockCodeSetupFragment(), false, this);
                    return true;
                } else if (preference == findPreference(Common.LOCKING_TYPE_PATTERN)) {
                    Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, getActivity(), LockPatternActivity.class);
                    startActivityForResult(intent, Util.getPatternCode(-1));
                    return true;
                }
                break;
            case OPTIONS:
                if (preference == findPreference(Common.VIEW_LOGS)) {
                    launchFragment(new LogViewerFragment(), false, this);
                    return true;
                }
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getListView().setPadding(0, 0, 0, 0);
        //noinspection deprecation
        getListView().setOverscrollFooter(new ColorDrawable(getListView().getContext().getResources().getColor(
                !prefs.getBoolean(Common.USE_DARK_STYLE, false) ? R.color.default_window_background : R.color.default_window_background_dark)));
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
            InputStream inputStream;
            try {
                inputStream = getActivity().getContentResolver().openInputStream(uri);
                File destination = new File(getActivity().getApplicationInfo().dataDir + File.separator + "background" + File.separator + "image");
                if (destination.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    destination.delete();
                }
                assert inputStream != null;
                FileUtils.copyInputStreamToFile(inputStream, destination);
                inputStream.close();
            } catch (IOException | AssertionError e) {
                e.printStackTrace();
            }
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
