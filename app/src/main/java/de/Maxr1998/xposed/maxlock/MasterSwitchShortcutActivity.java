package de.Maxr1998.xposed.maxlock;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import de.Maxr1998.xposed.maxlock.ui.lock.KnockCodeFragment;
import de.Maxr1998.xposed.maxlock.ui.lock.PinFragment;

public class MasterSwitchShortcutActivity extends Activity implements AuthenticationSucceededListener {
    // Thanks https://github.com/nkahoang/screenstandby/blob/master/src/com/nkahoang/screenstandby/ShortcutOnActivity.java

    public FragmentManager fm;
    SharedPreferences pref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("FromShortcut", false)) {
            pref = PreferenceManager.getDefaultSharedPreferences(this);

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
            if (str.equals("1")) {
                setContentView(R.layout.activity_lock);
                fm = getFragmentManager();

                String lockingType = pref.getString(Common.LOCKING_TYPE, "");

                if (lockingType.equals(Common.KEY_PASSWORD)) {
                    Util.askPassword(this);
                } else if (lockingType.equals(Common.KEY_PIN)) {
                    Fragment frag = new PinFragment();
                    Bundle b = new Bundle(1);
                    b.putString(Common.INTENT_EXTRAS_PKG_NAME, getString(R.string.unlock_master_switch));
                    frag.setArguments(b);
                    fm.beginTransaction().replace(R.id.frame_container, frag).commit();
                } else if (lockingType.equals(Common.KEY_KNOCK_CODE)) {
                    Fragment frag = new KnockCodeFragment();
                    Bundle b = new Bundle(1);
                    b.putString(Common.INTENT_EXTRAS_PKG_NAME, getString(R.string.unlock_master_switch));
                    frag.setArguments(b);
                    fm.beginTransaction().replace(R.id.frame_container, frag).commit();
                } else {
                    Toast.makeText(this, getString(R.string.toast_no_locking_types), Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Util.setMasterSwitch(true, this);
                Toast.makeText(this, getString(R.string.toast_master_switch_on), Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Intent serviceIntent = new Intent(Intent.ACTION_MAIN);
            ComponentName name = new ComponentName(getPackageName(), ".MasterSwitchShortcutActivity");
            serviceIntent.setComponent(name);
            serviceIntent.putExtra("FromShortcut", true);
            Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            shortcutintent.putExtra("duplicate", false);
            shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Toggle Master Switch");
            Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_launcher);
            shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
            shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, serviceIntent);
            sendBroadcast(shortcutintent);
            finish();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        Util.setMasterSwitch(false, this);
        Toast.makeText(this, getString(R.string.toast_master_switch_off), Toast.LENGTH_LONG).show();
        finish();
    }
}
