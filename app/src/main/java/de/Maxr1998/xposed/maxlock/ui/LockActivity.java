/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2015  Maxr1998
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.Maxr1998.xposed.maxlock.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.NotificationHelper;
import de.Maxr1998.xposed.maxlock.util.Util;

public class LockActivity extends FragmentActivity implements AuthenticationSucceededListener {

    private SharedPreferences prefs;
    private String[] names;
    private boolean unlocked = false;
    private AlertDialog fakeDieDialog, reportDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String lockActivityMode = getIntent().getStringExtra(Common.LOCK_ACTIVITY_MODE);
        final boolean fakeCrash = (lockActivityMode != null && lockActivityMode.equals(Common.MODE_FAKE_CRASH)) ||
                (prefs.getBoolean(Common.ENABLE_FAKE_CRASH_ALL_APPS, false) && (lockActivityMode == null || !lockActivityMode.equals(Common.MODE_UNLOCK)));

        if (!fakeCrash) {
            if (prefs.getBoolean(Common.HIDE_STATUS_BAR, false)) {
                setTheme(R.style.TranslucentStatusBar_Full);
            } else {
                setTheme(R.style.TranslucentStatusBar);
            }
        } else {
            setTheme(R.style.FakeDieDialog);
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        super.onCreate(savedInstanceState);
        // Intent extras
        names = getIntent().getStringArrayExtra(Common.INTENT_EXTRAS_NAMES);

        if (!fakeCrash) {
            defaultSetup();
        } else {
            fakeDieSetup();
        }
    }

    private void defaultSetup() {
        // Authentication fragment/UI
        setContentView(R.layout.activity_lock);
        Fragment frag = new LockFragment();
        Bundle b = new Bundle(1);
        b.putStringArray(Common.INTENT_EXTRAS_NAMES, names);
        frag.setArguments(b);
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();
    }

    private void fakeDieSetup() {
        PackageManager pm = getPackageManager();
        ApplicationInfo requestPkgInfo;
        try {
            requestPkgInfo = pm.getApplicationInfo(names[0], 0);
        } catch (PackageManager.NameNotFoundException e) {
            requestPkgInfo = null;
        }

        String requestPkgFullName = (String) (requestPkgInfo != null ? pm.getApplicationLabel(requestPkgInfo) : "(unknown)");
        AlertDialog.Builder fakeDieDialogBuilder = new AlertDialog.Builder(this);
        fakeDieDialog = fakeDieDialogBuilder.setMessage(String.format(getResources().getString(R.string.dialog_text_fake_die_stopped), requestPkgFullName))
                .setNeutralButton(R.string.dialog_button_report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (prefs.getBoolean(Common.ENABLE_DIRECT_UNLOCK, false)) {
                            fakeDieDialog.dismiss();
                            launchLockView();
                            finish();
                        } else {
                            fakeDieDialog.dismiss();
                            final EditText input = new EditText(LockActivity.this);
                            input.setMinLines(2);
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                            AlertDialog.Builder reportDialogBuilder = new AlertDialog.Builder(LockActivity.this);
                            reportDialog = reportDialogBuilder.setView(input)
                                    .setTitle(R.string.dialog_title_report_problem)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (input.getText().toString().equals(prefs.getString(Common.FAKE_CRASH_INPUT, "start"))) {
                                                reportDialog.dismiss();
                                                launchLockView();
                                                finish();
                                            } else {
                                                Toast.makeText(LockActivity.this, "Thanks for your feedback", Toast.LENGTH_SHORT).show();
                                                onBackPressed();
                                            }
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            onBackPressed();
                                        }
                                    })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialogInterface) {
                                            onBackPressed();
                                        }
                                    })
                                    .create();
                            reportDialog.show();
                        }
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onBackPressed();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        onBackPressed();
                    }
                })
                .create();
        fakeDieDialog.show();
    }

    private void launchLockView() {
        Intent it = new Intent(this, LockActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        it.putExtra(Common.LOCK_ACTIVITY_MODE, Common.MODE_UNLOCK);
        it.putExtra(Common.INTENT_EXTRAS_NAMES, names);
        startActivity(it);
    }

    @Override
    public void onAuthenticationSucceeded() {
        unlocked = true;
        NotificationHelper.postNotification(this);
        finish();
    }

    @Override
    public void onBackPressed() {
        Log.d("Xposed", "ML: Pressed back.");
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fakeDieDialog != null) {
            fakeDieDialog.dismiss();
        } else if (reportDialog != null) {
            reportDialog.dismiss();
        }
    }

    @Override
    protected void onStop() {
        if (prefs.getBoolean(Common.ENABLE_LOGGING, false) && !unlocked) {
            Util.logFailedAuthentication(this, names[0]);
        }
        super.onStop();
    }
}