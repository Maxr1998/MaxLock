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
import android.app.AlertDialog;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.preference.PreferenceFragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.TextView;

import com.commonsware.cwac.anddown.AndDown;
import com.haibison.android.lockpattern.LockPatternActivity;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.Maxr1998.xposed.maxlock.BillingHelper;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;


public class SettingsFragment extends PreferenceFragment {
    static Preference uninstall;
    static SharedPreferences prefs, prefsKeys, prefsTheme;
    Activity mActivity;
    BillingHelper billingHelper;
    static boolean proVersion;
    DevicePolicyManager devicePolicyManager;
    ComponentName deviceAdmin;

    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        //noinspection deprecation
        getPreferenceManager().setSharedPreferencesMode(Activity.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.preferences_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefsKeys = getActivity().getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        //noinspection deprecation
        prefsTheme = getActivity().getSharedPreferences(Common.PREFS_THEME, Context.MODE_WORLD_READABLE);

        devicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(getActivity(), UninstallProtectionReceiver.class);
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        boolean donated = !billingHelper.getBp().listOwnedProducts().isEmpty();
        proVersion = prefs.getBoolean(Common.ENABLE_PRO, false);
        String version = null;
        try {
            version = " v" + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Preference about = findPreference(Common.ABOUT);
        CheckBoxPreference ep = (CheckBoxPreference) findPreference(Common.ENABLE_PRO);
        String appName;
        if (donated) {
            appName = getString(R.string.app_name_pro);
            prefs.edit().putBoolean(Common.ENABLE_PRO, true).apply();
            proVersion = true;
            ep.setEnabled(false);
            ep.setChecked(true);
        } else {
            if (proVersion)
                appName = getString(R.string.app_name_pseudo_pro);
            else appName = getString(R.string.app_name);
        }
        getActivity().setTitle(appName);
        about.setTitle(appName + version);
        uninstall = findPreference(Common.UNINSTALL);
        if (isDeviceAdminActive()) {
            uninstall.setTitle(R.string.uninstall);
            uninstall.setSummary("");
        }
        startup();
        return super.onCreateView(paramLayoutInflater, paramViewGroup, paramBundle);
    }

    public void startup() {
        if (prefs.getBoolean(Common.FIRST_START, true)) {
            showAbout();
            prefs.edit().putBoolean(Common.FIRST_START, false).apply();
        }
        rateDialog();
        if (prefs.getString(Common.LOCKING_TYPE, "").equals("") && !new File(Util.dataDir(getActivity()) + File.separator + "shared_prefs" + File.separator + Common.PREFS_PACKAGES + ".xml").exists()) {
            SnackbarManager.show(Snackbar.with(getActivity()).type(SnackbarType.MULTI_LINE).duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE).swipeToDismiss(false).text(getString(R.string.no_locking_type) + getString(R.string.no_locked_apps)));
        } else if (prefs.getString(Common.LOCKING_TYPE, "").equals("")) {
            SnackbarManager.show(Snackbar.with(getActivity()).duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE).swipeToDismiss(false).text(R.string.no_locking_type));
        } else if (!new File(Util.dataDir(getActivity()) + File.separator + "shared_prefs" + File.separator + Common.PREFS_PACKAGES + ".xml").exists()) {
            SnackbarManager.show(Snackbar.with(getActivity()).duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE).swipeToDismiss(false).text(R.string.no_locked_apps));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        if (billingHelper != null) billingHelper.finish();
        mActivity = activity;
        billingHelper = new BillingHelper(mActivity);
        super.onAttach(activity);
    }

    @Override
    public void onDestroy() {
        billingHelper.finish();
        super.onDestroy();
    }

    public void rateDialog() {
        if (!prefs.contains(Common.FIRST_START_TIME))
            prefs.edit().putLong(Common.FIRST_START_TIME, System.currentTimeMillis()).apply();

        if (!prefs.getBoolean(Common.DIALOG_SHOW_NEVER, false) && System.currentTimeMillis() - prefs.getLong(Common.FIRST_START_TIME, System.currentTimeMillis()) > 10 * 24 * 3600 * 1000) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            @SuppressLint("InflateParams") View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_like_app, null);
            final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.dialog_cb_never_again);
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (checkBox.isChecked())
                        prefs.edit().putBoolean(Common.DIALOG_SHOW_NEVER, true).apply();
                    switch (i) {
                        case -3:
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Common.PKG_NAME)));
                            } catch (android.content.ActivityNotFoundException e) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + Common.PKG_NAME)));
                            }
                            break;
                        case -1:
                            billingHelper.showDialog();
                            break;
                    }
                    prefs.edit().putLong(Common.FIRST_START_TIME, System.currentTimeMillis()).apply();
                }
            };
            builder.setTitle(R.string.dialog_like_app)
                    .setView(dialogView)
                    .setPositiveButton(R.string.dialog_button_donate, onClickListener)
                    .setNeutralButton(R.string.dialog_button_rate, onClickListener)
                    .setNegativeButton(android.R.string.cancel, onClickListener)
                    .create().show();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference == findPreference(Common.LOCKING_TYPE_SETTINGS)) {
            launchFragment(new LockingTypeSettingsFragment(), true, this);
            return true;
        } else if (preference == findPreference(Common.LOCKING_UI_SETTINGS)) {
            launchFragment(new LockingUISettingsFragment(), true, this);
            return true;
        } else if (preference == findPreference(Common.LOCKING_OPTIONS)) {
            prefs.edit().putBoolean(Common.ENABLE_LOGGING, proVersion).apply();
            launchFragment(new LockingOptionsFragment(), true, this);
            return true;
        } else if (preference == findPreference(Common.TRUSTED_DEVICES)) {
            launchFragment(new TrustedDevicesFragment(), true, this);
            return true;
        } else if (preference == findPreference(Common.CHOOSE_APPS)) {
            billingHelper.finish();
            launchFragment(new AppsListFragment(), true, this);
            return true;
        } else if (preference == findPreference(Common.USE_DARK_STYLE) || preference == findPreference(Common.ENABLE_PRO)) {
            ((SettingsActivity) getActivity()).restart();
            return true;
        } else if (preference == findPreference(Common.ABOUT)) {
            showAbout();
            return true;
        } else if (preference == findPreference(Common.DONATE)) {
            billingHelper.showDialog();
            return true;
        } else if (preference == findPreference(Common.UNINSTALL)) {
            if (!isDeviceAdminActive()) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
                startActivity(intent);
            } else {
                devicePolicyManager.removeActiveAdmin(deviceAdmin);
            }
            return true;
        }
        return false;
    }

    public void showAbout() {
        AlertDialog.Builder about = new AlertDialog.Builder(getActivity());
        WebView webView = new WebView(getActivity());
        String markdown = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getActivity().getAssets().open("about.md")));
            String line;
            while ((line = br.readLine()) != null) {
                markdown = markdown + line + "\n";
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String html = new AndDown().markdownToHtml(markdown);
        webView.loadData(html, "text/html; charset=UTF-8", null);
        about.setView(webView).create().show();
    }

    public static void launchFragment(Fragment fragment, boolean fromRoot, Fragment from) {
        if (fromRoot) {
            if (Util.noGingerbread())
                from.getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            else
                from.getFragmentManager().popBackStack();
        }
        from.getFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).addToBackStack(null).commit();
        if (from.getFragmentManager().findFragmentById(R.id.settings_fragment) != null)
            from.getFragmentManager().beginTransaction().show(from.getFragmentManager().findFragmentById(R.id.settings_fragment)).commit();
    }

    private boolean isDeviceAdminActive() {
        return devicePolicyManager.isAdminActive(deviceAdmin);
    }

    public static class UninstallProtectionReceiver extends DeviceAdminReceiver {
        @Override
        public void onEnabled(Context context, Intent intent) {
            super.onEnabled(context, intent);
            uninstall.setTitle(R.string.uninstall);
            uninstall.setSummary("");
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
            super.onDisabled(context, intent);
            uninstall.setTitle(R.string.prevent_uninstall);
            uninstall.setSummary(R.string.prevent_uninstall_summary);
            Intent uninstall = new Intent(Intent.ACTION_DELETE);
            uninstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            uninstall.setData(Uri.parse("package:de.Maxr1998.xposed.maxlock"));
            context.startActivity(uninstall);
        }
    }

    public static class LockingTypeSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            addPreferencesFromResource(R.xml.preferences_locking_type);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            super.onPreferenceTreeClick(preferenceScreen, preference);
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
            return false;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case Util.PATTERN_CODE: {
                    if (resultCode == LockPatternActivity.RESULT_OK) {
                        Util.receiveAndSetPattern(getActivity(), data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN), null);
                    }
                    break;
                }
            }
        }
    }

    public static class LockingUISettingsFragment extends PreferenceFragment {
        private static final int READ_REQUEST_CODE = 42;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            addPreferencesFromResource(R.xml.preferences_locking_ui);

            Preference[] overriddenByTheme = {findPreference(Common.BACKGROUND), findPreference(Common.HIDE_TITLE_BAR), findPreference(Common.HIDE_INPUT_BAR), findPreference(Common.SHOW_DIVIDERS), findPreference(Common.TOUCH_VISIBLE)};
            if (prefsTheme.contains(Common.THEME_PKG)) {
                Preference themeManager = findPreference(Common.OPEN_THEME_MANAGER);
                themeManager.setSummary(getString(R.string.open_theme_manager_summary_applied) + prefsTheme.getString(Common.THEME_PKG, ""));
                for (Preference preference : overriddenByTheme) {
                    preference.setEnabled(false);
                    preference.setSummary(preference.getSummary() != null ? preference.getSummary() : " " + getString(R.string.overridden_by_theme));
                }
            }
            ListPreference lp = (ListPreference) findPreference(Common.BACKGROUND);
            findPreference(Common.BACKGROUND_COLOR).setEnabled(lp.getValue().equals("color"));
            lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (preference.getKey().equals(Common.BACKGROUND)) {
                        if (newValue.toString().equals("custom")) {
                            findPreference(Common.BACKGROUND_COLOR).setEnabled(false);
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent, READ_REQUEST_CODE);
                        } else if (newValue.toString().equals("color")) {
                            findPreference(Common.BACKGROUND_COLOR).setEnabled(true);
                        } else {
                            findPreference(Common.BACKGROUND_COLOR).setEnabled(false);
                        }
                    }
                    return true;
                }
            });
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            super.onPreferenceTreeClick(preferenceScreen, preference);
            if (preference == findPreference(Common.OPEN_THEME_MANAGER)) {
                Intent themeManager = new Intent();
                themeManager.setComponent(new ComponentName("de.Maxr1998.maxlock.thememanager", "de.Maxr1998.maxlock.thememanager" + ".MainActivity"));
                themeManager.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(themeManager);
                return true;
            }
            return false;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                Uri uri = null;
                InputStream inputStream;
                if (data != null) {
                    uri = data.getData();
                }
                if (uri == null) {
                    throw new NullPointerException();
                }
                try {
                    inputStream = getActivity().getContentResolver().openInputStream(uri);
                    File destination = new File(getActivity().getApplicationInfo().dataDir + File.separator + "background" + File.separator + "image");
                    if (destination.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        destination.delete();
                    }
                    FileUtils.copyInputStreamToFile(inputStream, destination);
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class LockingOptionsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            addPreferencesFromResource(R.xml.preferences_locking_options);
            Preference el = findPreference(Common.ENABLE_LOGGING);
            el.setEnabled(proVersion);
            if (!proVersion) {
                el.setSummary(R.string.toast_pro_required);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            super.onPreferenceTreeClick(preferenceScreen, preference);
            if (preference == findPreference(Common.VIEW_LOGS)) {
                launchFragment(new LogViewerFragment(), false, this);
                return true;
            }
            return false;
        }
    }

    public static class LogViewerFragment extends Fragment {
        private TextView textView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            textView = new TextView(getActivity()) {
                {
                    setVerticalScrollBarEnabled(true);
                    setMovementMethod(ScrollingMovementMethod.getInstance());
                    setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                }
            };
            try {
                BufferedReader br = new BufferedReader(new FileReader(getActivity().getApplicationInfo().dataDir + File.separator + Common.LOG_FILE));
                String line;
                while ((line = br.readLine()) != null) {
                    textView.append(line);
                    textView.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return textView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.logviewer_menu, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.delete_log) {
                File file = new File(getActivity().getApplicationInfo().dataDir + File.separator + Common.LOG_FILE);
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                textView.setText("");
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}