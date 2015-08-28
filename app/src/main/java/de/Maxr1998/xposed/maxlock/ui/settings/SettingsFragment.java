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
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.haibison.android.lockpattern.LockPatternActivity;
import com.nispok.snackbar.SnackbarManager;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.KnockCodeSetupFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.MaxLockPreferenceFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.PinSetupFragment;
import de.Maxr1998.xposed.maxlock.util.Util;

public class SettingsFragment extends MaxLockPreferenceFragment {
    private static Preference UNINSTALL;
    private static SharedPreferences PREFS_THEME;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdmin;

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        addPreferencesFromResource(R.xml.preferences_main);
        PREFS_THEME = getActivity().getSharedPreferences(Common.PREFS_THEME, Context.MODE_WORLD_READABLE);

        devicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(getActivity(), UninstallProtectionReceiver.class);
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        setupPro();
        UNINSTALL = findPreference(Common.UNINSTALL);
        if (isDeviceAdminActive()) {
            UNINSTALL.setTitle(R.string.pref_uninstall);
            UNINSTALL.setSummary("");
        }
        return super.onCreateView(paramLayoutInflater, paramViewGroup, paramBundle);
    }

    private void setupPro() {
        SwitchPreference ep = (SwitchPreference) findPreference(Common.ENABLE_PRO);
        if (Util.isDevMode()) {
            title = getString(R.string.app_name) + " Indev";
        } else if (false) {
            title = getString(R.string.app_name_pro);
            prefs.edit().putBoolean(Common.ENABLE_PRO, true).apply();
            ep.setEnabled(false);
            ep.setChecked(true);
        } else if (prefs.getBoolean(Common.ENABLE_PRO, false)) {
            title = getString(R.string.app_name_pseudo_pro);
        } else {
            title = getString(R.string.app_name);
        }
        String version;
        try {
            version = " v" + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "*Error*";
            e.printStackTrace();
        }
        findPreference(Common.ABOUT).setTitle(title + version);
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
            prefs.edit().putBoolean(Common.ENABLE_LOGGING, prefs.getBoolean(Common.ENABLE_PRO, false)).apply();
            launchFragment(new LockingOptionsFragment(), true, this);
            return true;
        } else if (preference == findPreference(Common.IMOD_OPTIONS)) {
            launchFragment(new LockingIntikaFragment(), true, this);
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
            Util.showAbout(getActivity());
            return true;
        } else if (preference == findPreference(Common.DONATE)) {

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

    private boolean isDeviceAdminActive() {
        return devicePolicyManager.isAdminActive(deviceAdmin);
    }

    public static class UninstallProtectionReceiver extends DeviceAdminReceiver {
        @Override
        public void onEnabled(Context context, Intent intent) {
            super.onEnabled(context, intent);
            if (UNINSTALL != null) {
                UNINSTALL.setTitle(R.string.pref_uninstall);
                UNINSTALL.setSummary("");
            }
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
            super.onDisabled(context, intent);
            if (UNINSTALL != null) {
                UNINSTALL.setTitle(R.string.pref_prevent_uninstall);
                UNINSTALL.setSummary(R.string.pref_prevent_uninstall_summary);
                Intent uninstall = new Intent(Intent.ACTION_DELETE);
                uninstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                uninstall.setData(Uri.parse("package:de.Maxr1998.xposed.maxlock"));
                context.startActivity(uninstall);
            }
        }
    }

    public static class LockingTypeSettingsFragment extends MaxLockPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            title = getString(R.string.pref_screen_locking_type);
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
                SnackbarManager.dismiss();
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
                        SnackbarManager.dismiss();
                    }
                    break;
                }
            }
        }
    }

    public static class LockingUISettingsFragment extends MaxLockPreferenceFragment {
        private static final int READ_REQUEST_CODE = 42;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            title = getString(R.string.pref_screen_locking_ui);
            addPreferencesFromResource(R.xml.preferences_locking_ui);

            Preference[] overriddenByTheme = {findPreference(Common.BACKGROUND), findPreference(Common.HIDE_TITLE_BAR), findPreference(Common.HIDE_INPUT_BAR), findPreference(Common.SHOW_KC_DIVIDERS), findPreference(Common.MAKE_KC_TOUCH_VISIBLE)};
            if (PREFS_THEME.contains(Common.THEME_PKG)) {
                Preference themeManager = findPreference(Common.OPEN_THEME_MANAGER);
                themeManager.setSummary(getString(R.string.pref_open_theme_manager_summary_applied) + PREFS_THEME.getString(Common.THEME_PKG, ""));
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
            Preference tabletMode = findPreference(Common.OVERRIDE_TABLET_MODE);
            tabletMode.setSummary(String.format(getString(R.string.pref_use_tablet_mode_summary),
                    Build.MODEL, getResources().getBoolean(R.bool.tablet_mode_default) ? "tablet/phablet" : "phone",
                    (int) getResources().getDisplayMetrics().xdpi, Math.min(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels)));
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
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
    }

    public static class LockingOptionsFragment extends MaxLockPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            title = getString(R.string.pref_screen_locking_options);
            addPreferencesFromResource(R.xml.preferences_locking_options);
            Preference el = findPreference(Common.ENABLE_LOGGING);
            el.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
            if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
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

    public static class LockingIntikaFragment extends MaxLockPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            title = getString(R.string.pref_screen_locking_intika);
            getPreferenceManager().setSharedPreferencesName(Common.PREFS_APPS);
            addPreferencesFromResource(R.xml.preferences_locking_imod);
            //Intika I.MoD - Loading check pro
            Preference iimod_enabled_g = findPreference(Common.ENABLE_IMOD_DELAY_GLOBAL);
            Preference iimod_enabled_p = findPreference(Common.ENABLE_IMOD_DELAY_APP);
            iimod_enabled_g.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
            iimod_enabled_p.setEnabled(prefs.getBoolean(Common.ENABLE_PRO, false));
            if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                //Intika I.MoD - Loading check pro
                iimod_enabled_g.setTitle(R.string.pref_delay_needpro);
                iimod_enabled_p.setTitle(R.string.pref_delay_needpro);
            }
        }
    }

    public static class LogViewerFragment extends Fragment {

        private RecyclerView mLogRecycler;
        private TextView mEmptyText;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            getActivity().setTitle(getString(R.string.pref_screen_logs));
            View rootView = inflater.inflate(R.layout.fragment_logs, container, false);
            mLogRecycler = (RecyclerView) rootView.findViewById(R.id.log_recycler);
            mEmptyText = (TextView) rootView.findViewById(R.id.logs_empty_text);
            List<String> text = new ArrayList<>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(getActivity().getApplicationInfo().dataDir + File.separator + Common.LOG_FILE));
                String line;
                while ((line = br.readLine()) != null) {
                    text.add(line);
                }
            } catch (FileNotFoundException e) {
                mLogRecycler.setVisibility(View.GONE);
                mEmptyText.setVisibility(View.VISIBLE);
                return rootView;
            } catch (IOException e) {
                e.printStackTrace();
            }
            mLogRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
            mLogRecycler.setItemAnimator(new DefaultItemAnimator());
            LogRecyclerAdapter adapter = new LogRecyclerAdapter(text);
            mLogRecycler.setAdapter(adapter);
            return rootView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.logviewer_menu, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.toolbar_delete_log) {
                File file = new File(getActivity().getApplicationInfo().dataDir + File.separator + Common.LOG_FILE);
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                mLogRecycler.setVisibility(View.GONE);
                mEmptyText.findViewById(R.id.logs_empty_text).setVisibility(View.VISIBLE);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        private static class LogRecyclerAdapter extends RecyclerView.Adapter<LogRecyclerAdapter.LogViewHolder> {

            private List<String> data;

            public LogRecyclerAdapter(@NonNull List<String> d) {
                data = d;
            }

            @Override
            public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_log_item, parent, false);
                return new LogViewHolder(v);
            }

            @Override
            public void onBindViewHolder(LogViewHolder holder, int p) {
                int position = holder.getLayoutPosition();
                String mCurrent = data.get(position);
                boolean showDate;
                if (position < 1) {
                    showDate = true;
                } else {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                        showDate = sdf.parse(mCurrent.substring(1, 9)).getTime() > sdf.parse(data.get(position - 1).substring(1, 9)).getTime();
                    } catch (ParseException e) {
                        showDate = false;
                        e.printStackTrace();
                    }
                }
                holder.mDate.setVisibility(showDate ? View.VISIBLE : View.GONE);
                holder.mDate.setText(showDate ? mCurrent.substring(1, 9).replace('/', '.') : "");
                holder.mTime.setText(mCurrent.substring(11, 19));
                holder.mAppName.setText(mCurrent.substring(21));
            }

            @Override
            public int getItemCount() {
                return data.size();
            }

            protected static class LogViewHolder extends RecyclerView.ViewHolder {

                protected TextView mDate, mTime, mAppName;

                public LogViewHolder(View itemView) {
                    super(itemView);
                    mDate = (TextView) itemView.findViewById(R.id.log_item_date);
                    mTime = (TextView) itemView.findViewById(R.id.log_item_time);
                    mAppName = (TextView) itemView.findViewById(R.id.log_item_app_name);
                }
            }
        }
    }
}
