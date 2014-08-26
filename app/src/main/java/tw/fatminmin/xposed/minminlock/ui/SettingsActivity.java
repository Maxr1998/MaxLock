package tw.fatminmin.xposed.minminlock.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import tw.fatminmin.xposed.minminlock.Common;
import tw.fatminmin.xposed.minminlock.R;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    public static SharedPreferences mainPref;
    private static String SET_PASSWORD = "set_password";
    private static String CHOOSE_APPS = "choose_apps";
    private SharedPreferences pref;
    private AlertDialog dialog;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getSharedPreferences(Common.KEY_PASSWORD, MODE_PRIVATE);
        mainPref = getSharedPreferences(getPackageName() + "_preferences", MODE_WORLD_READABLE);

        if (pref.getString("password", "").length() == 0) {
            setPassword();
        } else {
            askPassword();
        }

        addPreferencesFromResource(R.xml.preferences);

        Preference sp = findPreference(SET_PASSWORD);
        sp.setOnPreferenceClickListener(this);
        Preference ca = findPreference(CHOOSE_APPS);
        ca.setOnPreferenceClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null) {
            dialog.cancel();
        }
    }

    private void askPassword() {
        final EditText input = new EditText(SettingsActivity.this);

        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        dialog = new AlertDialog.Builder(SettingsActivity.this)
                .setCancelable(false)
                .setTitle("Password")
                .setView(input)
                .setPositiveButton("Ok", null)
                .create();
        dialog.show();

        ((ViewGroup) input.getParent()).setPadding(10, 10, 10, 10);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String val = input.getText().toString();
                if (val.equals(pref.getString(Common.KEY_PASSWORD, ""))) {
                    dialog.dismiss();
                } else {
                    input.setText("");
                    Toast.makeText(SettingsActivity.this, getString(R.string.msg_password_incorrect), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setPassword() {
        LayoutInflater inflater = LayoutInflater.from(SettingsActivity.this);
        final View rootView = inflater.inflate(R.layout.set_password, null);

        final AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
                .setCancelable(false)
                .setTitle("Set Password")
                .setView(rootView)
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", null)
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
                    Toast.makeText(SettingsActivity.this, R.string.msg_password_inconsistent, Toast.LENGTH_SHORT)
                            .show();
                } else if (v1.length() == 0) {
                    Toast.makeText(SettingsActivity.this, R.string.msg_password_null, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    dialog.dismiss();
                    pref.edit()
                            .putString(Common.KEY_PASSWORD, v1)
                            .commit();
                    Toast.makeText(SettingsActivity.this, R.string.msg_password_changed, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(pref.getString(Common.KEY_PASSWORD, "").length() == 0)) {
                    dialog.dismiss();
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.msg_password_null, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == findPreference(SET_PASSWORD)) {
            setPassword();
        } else if (preference == findPreference(CHOOSE_APPS)) {
            Intent intent = new Intent(this, AppsListActivity.class);
            startActivity(intent);
        }

        return false;
    }
}
