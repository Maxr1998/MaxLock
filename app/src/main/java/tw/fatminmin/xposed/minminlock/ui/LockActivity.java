package tw.fatminmin.xposed.minminlock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
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

import tw.fatminmin.xposed.minminlock.AuthenticationSucceededListener;
import tw.fatminmin.xposed.minminlock.Common;
import tw.fatminmin.xposed.minminlock.R;
import tw.fatminmin.xposed.minminlock.Util;

public class LockActivity extends Activity implements AuthenticationSucceededListener {

    public FragmentManager fm;
    private SharedPreferences pref, packagePref;
    private String requestPkg;
    private AlertDialog dialog;
    private ActivityManager am;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lock);

        pref = getSharedPreferences(Common.PREF, MODE_WORLD_READABLE);
        packagePref = getSharedPreferences(Common.PREF_PACKAGE, MODE_WORLD_READABLE);

        requestPkg = getIntent().getStringExtra(Common.KEY_APP_ACCESS);

        am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(requestPkg);
        am.killBackgroundProcesses("tw.fatminmin.xposed.minminlock");

        Long timestamp = System.currentTimeMillis();
        Long permitTimestamp = packagePref.getLong(requestPkg + "_tmp", 0);
        if (permitTimestamp != 0 && timestamp - permitTimestamp <= 10000) {
            Intent it = LockActivity.this.getPackageManager().getLaunchIntentForPackage(requestPkg);
            it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            LockActivity.this.startActivity(it);
            finish();
        } else {
            authenticate();
        }

    }

    private void authenticate() {
        fm = getFragmentManager();

        String lockType = pref.getString(Common.LOCK_TYPE, "");

        if (lockType.equals(Common.KEY_PASSWORD)) {
            final EditText input = new EditText(LockActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            dialog = new AlertDialog.Builder(LockActivity.this)
                    .setCancelable(false)
                    .setTitle(R.string.lock_type_password)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            dialog.show();

            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            ((ViewGroup) input.getParent()).setPadding(10, 10, 10, 10);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    am.killBackgroundProcesses(requestPkg);
                    am.killBackgroundProcesses("tw.fatminmin.xposed.minminlock");

                    String val = input.getText().toString();
                    if (Util.checkInput(val, Common.KEY_PASSWORD, LockActivity.this)) {
                        dialog.dismiss();
                        packagePref.edit()
                                .putLong(requestPkg + "_tmp", System.currentTimeMillis())
                                .commit();

                        am.killBackgroundProcesses(requestPkg);
                        am.killBackgroundProcesses("tw.fatminmin.xposed.minminlock");
                        Intent it = LockActivity.this.getPackageManager().getLaunchIntentForPackage(requestPkg);
                        it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        LockActivity.this.startActivity(it);
                        finish();
                    } else {
                        input.setText("");
                        Toast.makeText(LockActivity.this, getString(R.string.msg_password_incorrect), Toast.LENGTH_SHORT).show();

                    }
                }
            });
        } else if (lockType.equals(Common.KEY_PIN)) {

        } else if (lockType.equals(Common.KEY_KNOCK_CODE)) {
            fm.beginTransaction().replace(R.id.frame_container, new KnockCodeFragment()).commit();

        }
    }

    @Override
    public void onAuthenticationSucceeded(String tag) {
        packagePref.edit()
                .putLong(requestPkg + "_tmp", System.currentTimeMillis())
                .commit();

        am.killBackgroundProcesses(requestPkg);
        am.killBackgroundProcesses("tw.fatminmin.xposed.minminlock");
        Intent it = LockActivity.this.getPackageManager().getLaunchIntentForPackage(requestPkg);
        it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        LockActivity.this.startActivity(it);
        finish();
    }
}
