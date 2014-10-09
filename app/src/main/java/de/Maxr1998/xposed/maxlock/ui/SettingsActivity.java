package de.Maxr1998.xposed.maxlock.ui;

import android.app.Activity;
import android.app.Fragment;
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

        // Set theme
        if (pref.getBoolean(Common.USE_DARK_STYLE, false)) {
            setTheme(R.style.AppTheme_Dark);
        }
        setContentView(R.layout.activity_settings);

        fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.frame_container, new SettingsFragment()).commit();

        String lockingType = pref.getString(Common.LOCKING_TYPE, "");
        if (lockingType.equals(Common.KEY_PIN) && savedInstanceState == null) {
            // Show string fragment
        } else if (lockingType.equals(Common.KEY_KNOCK_CODE) && savedInstanceState == null) {
            Fragment frag = new KnockCodeFragment();
            Bundle b = new Bundle(1);
            b.putString(Common.INTENT_EXTRAS_PKG_NAME, getApplicationContext().getPackageName());
            frag.setArguments(b);
            fm.beginTransaction().replace(R.id.frame_container, frag).commit();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        fm.beginTransaction().replace(R.id.frame_container, new SettingsFragment()).commit();
    }

}
