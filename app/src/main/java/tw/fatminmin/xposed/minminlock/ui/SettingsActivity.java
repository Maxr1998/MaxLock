package tw.fatminmin.xposed.minminlock.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import tw.fatminmin.xposed.minminlock.AuthenticationSucceededListener;
import tw.fatminmin.xposed.minminlock.Common;
import tw.fatminmin.xposed.minminlock.R;

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
