package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;

public class MasterSwitchShortcutActivity extends FragmentActivity implements AuthenticationSucceededListener {

    SharedPreferences prefsPackages;

    @SuppressLint({"CommitPrefEdits", "WorldReadableFiles"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection deprecation
        prefsPackages = getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE);

        if (prefsPackages.getBoolean(Common.MASTER_SWITCH_ON, true)) {
            setContentView(R.layout.activity_lock);
            Fragment frag = new LockFragment();
            Bundle b = new Bundle(1);
            b.putString(Common.INTENT_EXTRAS_PKG_NAME, getString(R.string.unlock_master_switch));
            frag.setArguments(b);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();

        } else {
            prefsPackages.edit().putBoolean(Common.MASTER_SWITCH_ON, true).commit();
            Toast.makeText(this, getString(R.string.toast_master_switch_on), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onAuthenticationSucceeded() {
        prefsPackages.edit().putBoolean(Common.MASTER_SWITCH_ON, false).commit();
        Toast.makeText(this, getString(R.string.toast_master_switch_off), Toast.LENGTH_LONG).show();
        finish();
    }
}
