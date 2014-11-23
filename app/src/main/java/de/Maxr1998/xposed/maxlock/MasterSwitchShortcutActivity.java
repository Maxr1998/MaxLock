package de.Maxr1998.xposed.maxlock;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
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

            switch (lockingType) {
                case Common.KEY_PASSWORD:
                    Util.askPassword(this);
                    break;
                case Common.KEY_PIN: {
                    Fragment frag = new PinFragment();
                    Bundle b = new Bundle(1);
                    b.putString(Common.INTENT_EXTRAS_PKG_NAME, getString(R.string.unlock_master_switch));
                    frag.setArguments(b);
                    fm.beginTransaction().replace(R.id.frame_container, frag).commit();
                    break;
                }
                case Common.KEY_KNOCK_CODE: {
                    Fragment frag = new KnockCodeFragment();
                    Bundle b = new Bundle(1);
                    b.putString(Common.INTENT_EXTRAS_PKG_NAME, getString(R.string.unlock_master_switch));
                    frag.setArguments(b);
                    fm.beginTransaction().replace(R.id.frame_container, frag).commit();
                    break;
                }
                default:
                    Toast.makeText(this, getString(R.string.toast_no_locking_types), Toast.LENGTH_SHORT).show();
                    finish();
                    break;
            }
        } else {
            Util.setMasterSwitch(true, this);
            Toast.makeText(this, getString(R.string.toast_master_switch_on), Toast.LENGTH_LONG).show();
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
