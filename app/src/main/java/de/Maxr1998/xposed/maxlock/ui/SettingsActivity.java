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
    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Variables
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        Common.REQUEST_PKG = getApplicationContext().getPackageName();

        // Set theme
        if (pref.getBoolean(Common.USE_DARK_STYLE, false)) {
            setTheme(R.style.AppTheme_Dark);
        }
        setContentView(R.layout.activity_settings);

        fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.frame_container, new SettingsFragment()).commit();

        String lock_type = pref.getString(Common.LOCK_TYPE, "");
        if (lock_type.equals(Common.KEY_PIN) && savedInstanceState == null) {
            // Show string fragment
        } else if (lock_type.equals(Common.KEY_KNOCK_CODE) && savedInstanceState == null) {
            fm.beginTransaction().replace(R.id.frame_container, new KnockCodeFragment()).commit();
        }
    }

    @Override
    public void onAuthenticationSucceeded(String tag) {
        fm.beginTransaction().replace(R.id.frame_container, new SettingsFragment()).commit();
    }

}
