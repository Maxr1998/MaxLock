package de.Maxr1998.xposed.maxlock.ui.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
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
import android.support.v4.preference.PreferenceFragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.commonsware.cwac.anddown.AndDown;

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
    SharedPreferences pref, keysPref;
    BillingHelper billingHelper;
    boolean proVersion;
    DevicePolicyManager devicePolicyManager;
    ComponentName deviceAdmin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        //noinspection deprecation
        getPreferenceManager().setSharedPreferencesMode(Activity.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.preferences_main);
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        keysPref = getActivity().getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        billingHelper = new BillingHelper(getActivity());

        devicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(getActivity(), UninstallProtectionReceiver.class);
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        boolean donated = !billingHelper.getBp().listOwnedProducts().isEmpty();
        proVersion = pref.getBoolean(Common.ENABLE_PRO, false);
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
            pref.edit().putBoolean(Common.ENABLE_PRO, true).apply();
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
        return super.onCreateView(paramLayoutInflater, paramViewGroup, paramBundle);
    }

    @Override
    public void onDestroy() {
        billingHelper.finish();
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference == findPreference(Common.LOCKING_TYPE_SETTINGS)) {
            if (SettingsActivity.IS_DUAL_PANE) {
                cleanBackStack();
                getActivity().findViewById(R.id.frame_container_scd).setVisibility(View.VISIBLE);
                getFragmentManager().beginTransaction().replace(R.id.frame_container_scd, new LockingTypeSettingsFragment()).addToBackStack(null).commit();
            } else {
                getFragmentManager().beginTransaction().replace(R.id.frame_container, new LockingTypeSettingsFragment()).addToBackStack(null).commit();
            }
            return true;
        } else if (preference == findPreference(Common.LOCKING_UI_SETTINGS)) {
            if (SettingsActivity.IS_DUAL_PANE) {
                cleanBackStack();
                getActivity().findViewById(R.id.frame_container_scd).setVisibility(View.VISIBLE);
                getFragmentManager().beginTransaction().replace(R.id.frame_container_scd, new LockingUISettingsFragment()).addToBackStack(null).commit();
            } else {
                getFragmentManager().beginTransaction().replace(R.id.frame_container, new LockingUISettingsFragment()).addToBackStack(null).commit();
            }
            return true;
        } else if (preference == findPreference(Common.LOCKING_OPTIONS)) {
            pref.edit().putBoolean(Common.ENABLE_LOGGING, proVersion);
            if (SettingsActivity.IS_DUAL_PANE) {
                cleanBackStack();
                getActivity().findViewById(R.id.frame_container_scd).setVisibility(View.VISIBLE);
                getFragmentManager().beginTransaction().replace(R.id.frame_container_scd, new LockingOptionsFragment()).addToBackStack(null).commit();
            } else {
                getFragmentManager().beginTransaction().replace(R.id.frame_container, new LockingOptionsFragment()).addToBackStack(null).commit();
            }
            return true;
        } else if (preference == findPreference(Common.TRUSTED_DEVICES)) {
            if (SettingsActivity.IS_DUAL_PANE) {
                cleanBackStack();
                getActivity().findViewById(R.id.frame_container_scd).setVisibility(View.VISIBLE);
                getFragmentManager().beginTransaction().replace(R.id.frame_container_scd, new TrustedDevicesFragment()).addToBackStack(null).commit();
            } else {
                getFragmentManager().beginTransaction().replace(R.id.frame_container, new TrustedDevicesFragment()).addToBackStack(null).commit();
            }
            return true;
        } else if (preference == findPreference(Common.CHOOSE_APPS)) {
            if (SettingsActivity.IS_DUAL_PANE) {
                cleanBackStack();
                getActivity().findViewById(R.id.frame_container_scd).setVisibility(View.VISIBLE);
                getFragmentManager().beginTransaction().replace(R.id.frame_container_scd, new AppsListFragment()).addToBackStack(null).commit();
            } else {
                getFragmentManager().beginTransaction().replace(R.id.frame_container, new AppsListFragment()).addToBackStack(null).commit();
            }
            return true;
        } else if (preference == findPreference(Common.USE_DARK_STYLE) || preference == findPreference(Common.ENABLE_PRO)) {
            ((SettingsActivity) getActivity()).restart();
            return true;
        } else if (preference == findPreference(Common.ABOUT)) {
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

    @SuppressLint("InlinedApi")
    public void cleanBackStack() {
        if (Util.noGingerbread())
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        else
            getFragmentManager().popBackStack();
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

    @SuppressLint("ValidFragment")
    public class LockingTypeSettingsFragment extends PreferenceFragment {
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
                if (SettingsActivity.IS_DUAL_PANE) {
                    getFragmentManager().beginTransaction().replace(R.id.frame_container_scd, new PinSetupFragment()).addToBackStack(null).commit();
                } else {
                    getFragmentManager().beginTransaction().replace(R.id.frame_container, new PinSetupFragment()).addToBackStack(null).commit();
                }
                return true;
            } else if (preference == findPreference(Common.LOCKING_TYPE_KNOCK_CODE)) {
                if (SettingsActivity.IS_DUAL_PANE) {
                    getFragmentManager().beginTransaction().replace(R.id.frame_container_scd, new KnockCodeSetupFragment()).addToBackStack(null).commit();
                } else {
                    getFragmentManager().beginTransaction().replace(R.id.frame_container, new KnockCodeSetupFragment()).addToBackStack(null).commit();
                }
                return true;
            }
            return false;
        }
    }

    @SuppressLint("ValidFragment")
    public class LockingUISettingsFragment extends PreferenceFragment {

        private static final int READ_REQUEST_CODE = 42;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            addPreferencesFromResource(R.xml.preferences_locking_ui);

            ListPreference lp = (ListPreference) findPreference(Common.BACKGROUND);
            lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (preference.getKey().equals(Common.BACKGROUND) && newValue.toString().equals("custom")) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, READ_REQUEST_CODE);
                    }
                    return true;
                }
            });
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

    @SuppressLint("ValidFragment")
    public class LockingOptionsFragment extends PreferenceFragment {
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
                if (SettingsActivity.IS_DUAL_PANE) {
                    getFragmentManager().beginTransaction().replace(R.id.frame_container_scd, new LogViewerFragment()).addToBackStack(null).commit();
                } else {
                    getFragmentManager().beginTransaction().replace(R.id.frame_container, new LogViewerFragment()).addToBackStack(null).commit();
                }
                return true;
            }
            return false;
        }
    }

    @SuppressLint("ValidFragment")
    public class LogViewerFragment extends Fragment {

        private TextView textView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
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
