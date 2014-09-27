package de.Maxr1998.xposed.maxlock.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;


public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences pref, keysPref;
    private AlertDialog dialog;
    private Switch masterSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity().getActionBar() != null)
            getActivity().getActionBar().show();
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        keysPref = getActivity().getSharedPreferences(Common.PREF_KEYS, Activity.MODE_PRIVATE);

        if (pref.getString(Common.LOCK_TYPE, "").equals(Common.KEY_PASSWORD)) {
            if (keysPref.getString(Common.KEY_PASSWORD, "").length() == 0) {
                setPassword();
            } else {
                askPassword();
            }
        }

        Preference ltpw = findPreference(Common.LOCK_TYPE_PASSWORD);
        ltpw.setOnPreferenceClickListener(this);
        Preference ltkc = findPreference(Common.LOCK_TYPE_KNOCK_CODE);
        ltkc.setOnPreferenceClickListener(this);
        Preference ca = findPreference(Common.CHOOSE_APPS);
        ca.setOnPreferenceClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dialog != null) {
            dialog.cancel();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.menu, menu);

        View masterSwitchView = menu.findItem(R.id.master_switch_menu_item).getActionView().findViewById(R.id.master_switch);
        masterSwitch = (Switch) masterSwitchView;
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean masterSwitchPref = pref.getBoolean(Common.MASTER_SWITCH, true);

        masterSwitch.setChecked(masterSwitchPref);
        masterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                pref.edit().putBoolean(Common.MASTER_SWITCH, checked).commit();
            }
        });
    }

    private void askPassword() {

        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setTitle(R.string.lock_type_password)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        ((ViewGroup) input.getParent()).setPadding(10, 10, 10, 10);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String val = input.getText().toString();
                if (Util.checkInput(val, Common.KEY_PASSWORD, getActivity())) {
                    dialog.dismiss();
                } else {
                    input.setText("");
                    Toast.makeText(getActivity(), getString(R.string.msg_password_incorrect), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setPassword() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View rootView = inflater.inflate(R.layout.set_password, null);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setTitle("Set Password")
                .setView(rootView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        ((ViewGroup) rootView.getParent()).setPadding(10, 10, 10, 10);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText p1 = (EditText) rootView.findViewById(R.id.edt_password);
                EditText p2 = (EditText) rootView.findViewById(R.id.edt_re_password);
                String v1 = p1.getText().toString();
                String v2 = p2.getText().toString();

                if (!v1.equals(v2)) {
                    p1.setText("");
                    p2.setText("");
                    Toast.makeText(getActivity(), R.string.msg_password_inconsistent, Toast.LENGTH_SHORT)
                            .show();
                } else if (v1.length() == 0) {
                    Toast.makeText(getActivity(), R.string.msg_password_null, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    dialog.dismiss();
                    keysPref.edit()
                            .putString(Common.KEY_PASSWORD, Util.sha1Hash(v1))
                            .remove(Common.KEY_PIN)
                            .remove(Common.KEY_KNOCK_CODE)
                            .commit();
                    pref.edit()
                            .putString(Common.LOCK_TYPE, Common.KEY_PASSWORD)
                            .commit();
                    Toast.makeText(getActivity(), R.string.msg_password_changed, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(keysPref.getString(Common.LOCK_TYPE, "").equals("pw"))) {
                    dialog.dismiss();
                } else {
                    Toast.makeText(getActivity(), R.string.msg_password_null, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == findPreference(Common.LOCK_TYPE_PASSWORD)) {
            pref.edit()
                    .putString(Common.LOCK_TYPE, Common.KEY_PASSWORD)
                    .commit();
            setPassword();
        } else if (preference == findPreference(Common.LOCK_TYPE_KNOCK_CODE)) {
            getActivity().getFragmentManager().beginTransaction().replace(R.id.frame_container, new KnockCodeSetupFragment()).commit();
        } else if (preference == findPreference(Common.CHOOSE_APPS)) {
            Intent intent = new Intent(getActivity(), AppsListActivity.class);
            startActivity(intent);
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);

        if (key.equals(Common.HIDE_APP_FROM_LAUNCHER)) {
            CheckBoxPreference checkBoxPreference = (CheckBoxPreference) pref;
            if (checkBoxPreference.isChecked()) {
                PackageManager p = getActivity().getPackageManager();
                p.setComponentEnabledSetting(getActivity().getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } else {
                PackageManager p = getActivity().getPackageManager();
                p.setComponentEnabledSetting(getActivity().getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        }
    }
}
