package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.lib.StatusBarTintApi;
import de.Maxr1998.xposed.maxlock.ui.settings.SettingsFragment;


public class SettingsActivity extends ActionBarActivity implements AuthenticationSucceededListener {

    private static final String TAG_SETTINGS_FRAGMENT = "tag_settings_fragment";
    public static boolean IS_DUAL_PANE;
    public SettingsFragment mSettingsFragment;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Util.cleanUp(this);

        // Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_locking_type, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_locking_ui, false);
        if (prefs.getBoolean(Common.USE_DARK_STYLE, false)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        IS_DUAL_PANE = findViewById(R.id.frame_container_scd) != null;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSettingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(TAG_SETTINGS_FRAGMENT);
        if (mSettingsFragment == null) {
            getSupportActionBar().hide();
            Fragment frag = new LockFragment();
            Bundle b = new Bundle(1);
            b.putString(Common.INTENT_EXTRAS_PKG_NAME, getApplicationContext().getPackageName());
            frag.setArguments(b);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();
        }
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        SwitchCompat master_switch = (SwitchCompat) MenuItemCompat.getActionView(menu.findItem(R.id.master_switch_menu_item));
        master_switch.setChecked(getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE).getBoolean(Common.MASTER_SWITCH_ON, true));
        master_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint({"CommitPrefEdits"})
            @Override
            public void onCheckedChanged(CompoundButton button, boolean b) {
                getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE).edit().putBoolean(Common.MASTER_SWITCH_ON, b).commit();
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            if (findViewById(R.id.frame_container_scd) != null && getSupportFragmentManager().getBackStackEntryCount() == 1)
                findViewById(R.id.frame_container_scd).setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        if (mSettingsFragment == null) {
            mSettingsFragment = new SettingsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, mSettingsFragment, TAG_SETTINGS_FRAGMENT).commit();
            getSupportActionBar().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatusBarTintApi.sendColorChangeIntent(getResources().getColor(R.color.primary_red_dark), -3, getResources().getColor(R.color.black), -3, this);
    }
}
