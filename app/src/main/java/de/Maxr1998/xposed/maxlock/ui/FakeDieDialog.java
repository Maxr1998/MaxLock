package de.Maxr1998.xposed.maxlock.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;


public class FakeDieDialog extends Activity {

    private String requestPkg;
    private ApplicationInfo requestPkgInfo;
    private AlertDialog.Builder alertDialog, reportDialog;
    private SharedPreferences pref;
    private int flags;
    private Bundle extras;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        requestPkg = getIntent().getStringExtra(Common.INTENT_EXTRAS_PKG_NAME);
        flags = getIntent().getIntExtra(Common.INTENT_EXTRAS_FLAGS, 1);
        extras = getIntent().getBundleExtra(Common.INTENT_EXTRAS_BUNDLE_EXTRAS);

        ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        am.killBackgroundProcesses("de.Maxr1998.xposed.maxlock");
        PackageManager pm = getPackageManager();
        try {
            requestPkgInfo = pm.getApplicationInfo(requestPkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            requestPkgInfo = null;
        }
        String requestPkgFullName = (String) (requestPkgInfo != null ? pm.getApplicationLabel(requestPkgInfo) : "(unknown)");
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(String.format(getResources().getString(R.string.fake_die_message), requestPkgFullName))
                .setNeutralButton(R.string.report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        alertDialog.create().dismiss();
                        final EditText input = new EditText(FakeDieDialog.this);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        reportDialog = new AlertDialog.Builder(FakeDieDialog.this);
                        reportDialog.setView(input)
                                .setTitle(R.string.report_problem)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (input.getText().toString().equals(pref.getString(Common.FAKE_DIE_INPUT, "start"))) {
                                            Intent it = new Intent(FakeDieDialog.this, LockActivity.class);
                                            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            it.putExtra(Common.INTENT_EXTRAS_PKG_NAME, requestPkg);
                                            it.putExtra(Common.INTENT_EXTRAS_FLAGS, flags);
                                            it.putExtra(Common.INTENT_EXTRAS_BUNDLE_EXTRAS, extras);
                                            startActivity(it);
                                        }
                                        finish();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                })
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialogInterface) {
                                        finish();
                                    }
                                })
                                .create().show();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .create().show();
    }
}