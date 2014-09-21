package de.Maxr1998.xposed.maxlock.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;


public class SettingsActivity extends Activity implements AuthenticationSucceededListener {

    public FragmentManager fm;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Common.REQUEST_PKG = getApplicationContext().getPackageName();

        fm = getFragmentManager();

        if (savedInstanceState == null)
            fm.beginTransaction().replace(R.id.frame_container, new SettingsFragment()).commit();

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        String lock_type = pref.getString(Common.LOCK_TYPE, "");
        if (lock_type.equals(Common.KEY_PIN) & savedInstanceState == null) {
            // Show string fragment
        } else if (lock_type.equals(Common.KEY_KNOCK_CODE) & savedInstanceState == null) {
            fm.beginTransaction().replace(R.id.frame_container, new KnockCodeFragment()).commit();
        }
    }

    @Override
    public void onAuthenticationSucceeded(String tag) {
        fm.beginTransaction().replace(R.id.frame_container, new SettingsFragment()).commit();
    }

}
