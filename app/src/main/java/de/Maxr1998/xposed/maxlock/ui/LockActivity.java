package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.ui.lock.KnockCodeFragment;
import de.Maxr1998.xposed.maxlock.ui.lock.PinFragment;

public class LockActivity extends Activity implements AuthenticationSucceededListener {

    public FragmentManager fm;
    private SharedPreferences pref, packagePref;
    private String requestPkg;
    private ActivityManager am;
    private Intent app;
    private boolean isInFocus = false;

    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Preferences
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        packagePref = getSharedPreferences(Common.PREF_PACKAGE, MODE_WORLD_READABLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);


        requestPkg = getIntent().getStringExtra(Common.INTENT_EXTRAS_PKG_NAME);

        app = getIntent().getParcelableExtra(Common.INTENT_EXTRAS_INTENT);

        am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        am.killBackgroundProcesses("de.Maxr1998.xposed.maxlock");

        Util.getMasterSwitch(this);

        Long timestamp = System.currentTimeMillis();
        Long permitTimestamp = packagePref.getLong(requestPkg + "_tmp", 0);
        if (permitTimestamp != 0 && timestamp - permitTimestamp <= 10000) {
            onAuthenticationSucceeded();
        } else {
            authenticate();
        }
    }

    private void authenticate() {
        fm = getFragmentManager();

        String lockingType = pref.getString(Common.LOCKING_TYPE, "");

        switch (lockingType) {
            case Common.KEY_PASSWORD:
                Util.askPassword(this);
                break;
            case Common.KEY_PIN: {
                Fragment frag = new PinFragment();
                Bundle b = new Bundle(1);
                b.putString(Common.INTENT_EXTRAS_PKG_NAME, requestPkg);
                frag.setArguments(b);
                fm.beginTransaction().replace(R.id.frame_container, frag).commit();
                break;
            }
            case Common.KEY_KNOCK_CODE: {
                Fragment frag = new KnockCodeFragment();
                Bundle b = new Bundle(1);
                b.putString(Common.INTENT_EXTRAS_PKG_NAME, requestPkg);
                frag.setArguments(b);
                fm.beginTransaction().replace(R.id.frame_container, frag).commit();
                break;
            }
            default:
                onAuthenticationSucceeded();
                break;
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onAuthenticationSucceeded() {
        packagePref.edit()
                .putLong(requestPkg + "_tmp", System.currentTimeMillis())
                .commit();
        am.killBackgroundProcesses("de.Maxr1998.xposed.maxlock");
        Intent intent = new Intent(app);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startActivity(intent);
        } catch (SecurityException e) {
            Intent intent_option = getPackageManager().getLaunchIntentForPackage(requestPkg);
            intent_option.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent_option);
        } finally {
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        isInFocus = hasFocus;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!isInFocus) {
            Log.d("MaxLock/LockActivity", "Lost focus, finishing.");
            finish();
        }
    }
}
