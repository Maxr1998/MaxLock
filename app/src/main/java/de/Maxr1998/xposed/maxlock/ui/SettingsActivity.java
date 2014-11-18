package de.Maxr1998.xposed.maxlock.ui;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.lib.StatusBarTintApi;
import de.Maxr1998.xposed.maxlock.ui.lock.KnockCodeFragment;
import de.Maxr1998.xposed.maxlock.ui.lock.PinFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.SettingsFragment;


public class SettingsActivity extends ActionBarActivity implements AuthenticationSucceededListener {

    private static final String TAG_SETTINGS_FRAGMENT = "tag_settings_fragment";
    public static boolean IS_DUAL_PANE;
    SharedPreferences pref;
    private SettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Preferences
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_locking_type, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_locking_ui, false);
        if (pref.getBoolean(Common.USE_DARK_STYLE, false)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        IS_DUAL_PANE = findViewById(R.id.frame_container_scd) != null;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSettingsFragment = (SettingsFragment) getFragmentManager().findFragmentByTag(TAG_SETTINGS_FRAGMENT);
        if (mSettingsFragment == null) {
            String lockingType = pref.getString(Common.LOCKING_TYPE, "");
            getSupportActionBar().hide();
            if (lockingType.equals("")) {
                onAuthenticationSucceeded();
            } else if (lockingType.equals(Common.KEY_PASSWORD)) {
                Util.askPassword(this);
            } else if (lockingType.equals(Common.KEY_PIN)) {
                Fragment frag = new PinFragment();
                Bundle b = new Bundle(1);
                b.putString(Common.INTENT_EXTRAS_PKG_NAME, getApplicationContext().getPackageName());
                frag.setArguments(b);
                getFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();
            } else if (lockingType.equals(Common.KEY_KNOCK_CODE)) {
                Fragment frag = new KnockCodeFragment();
                Bundle b = new Bundle(1);
                b.putString(Common.INTENT_EXTRAS_PKG_NAME, getApplicationContext().getPackageName());
                frag.setArguments(b);
                getFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        SwitchCompat master_switch = (SwitchCompat) MenuItemCompat.getActionView(menu.findItem(R.id.master_switch_menu_item));
        String str = "1";
        try {
            File file = new File(getApplicationInfo().dataDir + File.separator + "master_switch");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            str = bufferedReader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (str == null) {
            Log.d("MasterSwitch", "File is empty!");
            str = "1";
        }
        master_switch.setChecked(str.equals("1"));
        master_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean b) {
                Util.setMasterSwitch(b, SettingsActivity.this);
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            if (findViewById(R.id.frame_container_scd) != null && getFragmentManager().getBackStackEntryCount() == 1)
                findViewById(R.id.frame_container_scd).setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        if (mSettingsFragment == null) {
            mSettingsFragment = new SettingsFragment();
            getFragmentManager().beginTransaction().replace(R.id.frame_container, mSettingsFragment, TAG_SETTINGS_FRAGMENT).commit();
            getSupportActionBar().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatusBarTintApi.sendColorChangeIntent(getResources().getColor(R.color.primary_red_dark), -3, getResources().getColor(R.color.black), -3, this);
    }
}
