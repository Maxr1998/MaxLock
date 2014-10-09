package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class LockActivity extends Activity implements AuthenticationSucceededListener {

    public FragmentManager fm;
    private SharedPreferences pref, packagePref;
    private String requestPkg;
    private AlertDialog dialog;
    private ActivityManager am;
    private int flags;
    private Bundle extras;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lock);

        pref = getSharedPreferences(Common.PREF, MODE_WORLD_READABLE);
        packagePref = getSharedPreferences(Common.PREF_PACKAGE, MODE_WORLD_READABLE);

        requestPkg = getIntent().getStringExtra(Common.INTENT_EXTRAS_PKG_NAME);
        flags = getIntent().getIntExtra(Common.INTENT_EXTRAS_FLAGS, 1);
        extras = getIntent().getBundleExtra(Common.INTENT_EXTRAS_BUNDLE_EXTRAS);

        am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        //am.killBackgroundProcesses(requestPkg);
        am.killBackgroundProcesses("de.Maxr1998.xposed.maxlock");


        Long timestamp = System.currentTimeMillis();
        Long permitTimestamp = packagePref.getLong(requestPkg + "_tmp", 0);
        if (permitTimestamp != 0 && timestamp - permitTimestamp <= 10000) {
            Intent it = LockActivity.this.getPackageManager().getLaunchIntentForPackage(requestPkg);
            //it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            LockActivity.this.startActivity(it);
            finish();
        } else {
            authenticate();
        }

    }

    private void authenticate() {
        fm = getFragmentManager();

        String lockingType = pref.getString(Common.LOCKING_TYPE, "");

        if (lockingType.equals(Common.KEY_PASSWORD)) {
            final EditText input = new EditText(LockActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            dialog = new AlertDialog.Builder(LockActivity.this)
                    .setCancelable(false)
                    .setTitle(R.string.locking_type_password)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            dialog.show();

            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            ((ViewGroup) input.getParent()).setPadding(10, 10, 10, 10);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String val = input.getText().toString();
                    if (Util.checkInput(val, Common.KEY_PASSWORD, LockActivity.this)) {
                        dialog.dismiss();
                        onAuthenticationSucceeded();
                    } else {
                        input.setText("");
                        Toast.makeText(LockActivity.this, getString(R.string.msg_password_incorrect), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (lockingType.equals(Common.KEY_PIN)) {
            int x = 1;
        } else if (lockingType.equals(Common.KEY_KNOCK_CODE)) {
            Fragment frag = new KnockCodeFragment();
            Bundle b = new Bundle(1);
            b.putString(Common.INTENT_EXTRAS_PKG_NAME, requestPkg);
            frag.setArguments(b);
            fm.beginTransaction().replace(R.id.frame_container, frag).commit();
        } else {
            onAuthenticationSucceeded();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        packagePref.edit()
                .putLong(requestPkg + "_tmp", System.currentTimeMillis())
                .commit();

        //am.killBackgroundProcesses(requestPkg);
        am.killBackgroundProcesses("de.Maxr1998.xposed.maxlock");
        Intent it = LockActivity.this.getPackageManager().getLaunchIntentForPackage(requestPkg);
        it.putExtras(extras);
        it.setFlags(/*Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK*/flags);
        LockActivity.this.startActivity(it);
        finish();
    }
}
