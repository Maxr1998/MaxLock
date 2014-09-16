package tw.fatminmin.xposed.minminlock.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import tw.fatminmin.xposed.minminlock.Common;
import tw.fatminmin.xposed.minminlock.R;
import tw.fatminmin.xposed.minminlock.Util;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private SharedPreferences pref, keysPref;
    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity().getActionBar() != null)
            getActivity().getActionBar().show();
        addPreferencesFromResource(R.xml.preferences);

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
                if (!(keysPref.getString(Common.KEY_PASSWORD, "").length() == 0)) {
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
            pref.edit()
                    .putString(Common.LOCK_TYPE, Common.KEY_KNOCK_CODE)
                    .commit();
            keysPref.edit()
                    .putString(Common.KEY_KNOCK_CODE, Util.sha1Hash("12243"))
                    .remove(Common.KEY_PASSWORD)
                    .remove(Common.KEY_PIN)
                    .commit();
            Toast.makeText(getActivity(), "Set Knock-Code Lock-Type", Toast.LENGTH_SHORT).show();
        } else if (preference == findPreference(Common.CHOOSE_APPS)) {
            Intent intent = new Intent(getActivity(), AppsListActivity.class);
            startActivity(intent);
        }
        return false;
    }
}
