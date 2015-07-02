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

import android.annotation.SuppressLint;
import android.app.Activity;
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
import de.Maxr1998.xposed.maxlock.LockHelper;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static de.Maxr1998.xposed.maxlock.LockHelper.launchLockView;

public class LockActivity extends FragmentActivity implements AuthenticationSucceededListener {

    private String packageName;
    private Intent original;
    private SharedPreferences prefs, prefsIMod, prefsTemp;
    private boolean isInFocus = false, unlocked = false;
    private AlertDialog fakeDieDialog, reportDialog;

    @SuppressLint("WorldReadableFiles")
    public static void directUnlock(Activity caller, Intent orig, String pkgName) {
        MLPreferences.getPrefsTemp(caller).edit()
                .putLong(pkgName + Common.FLAG_TMP, System.currentTimeMillis())
                .commit();
        try {
            orig.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            caller.startActivity(orig);
        } catch (Exception e) {
            Intent intent_option = caller.getPackageManager().getLaunchIntentForPackage(pkgName);
            intent_option.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            caller.startActivity(intent_option);
        } finally {
            caller.finish();
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String mode = getIntent().getStringExtra(Common.LOCK_ACTIVITY_MODE);
        switch (mode) {
            case Common.MODE_DEFAULT:
                if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.HIDE_STATUS_BAR, false)) {
                    setTheme(R.style.TranslucentStatusBar_Full);
                } else {
                    setTheme(R.style.TranslucentStatusBar);
                }
                break;
            case Common.MODE_FAKE_DIE:
                setTheme(R.style.FakeDieDialog);
                getWindow().setBackgroundDrawable(new ColorDrawable(0));
                break;
            default:
                super.onCreate(savedInstanceState);
                finish();
                return;
        }
        super.onCreate(savedInstanceState);
        // Intent extras
        packageName = getIntent().getStringExtra(Common.INTENT_EXTRAS_PKG_NAME);
        original = getIntent().getParcelableExtra(Common.INTENT_EXTRAS_INTENT);

        // Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsIMod = getSharedPreferences(Common.PREFS_IMOD, MODE_WORLD_READABLE);
        prefsTemp = MLPreferences.getPrefsTemp(this);

        long permitTimestamp = prefsTemp.getLong(packageName + Common.FLAG_TMP, 0);
        if (LockHelper.timerOrIMod(packageName, permitTimestamp, prefsIMod, prefsTemp)) {
            openApp();
            return;
        }

        switch (mode) {
            case Common.MODE_DEFAULT:
                defaultSetup();
                break;
            case Common.MODE_FAKE_DIE:
                fakeDieSetup();
                break;
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onAuthenticationSucceeded() {
        // Save time for Intika mod
        if (prefsIMod.getBoolean(Common.IMOD_DELAY_GLOBAL_ENABLED, false)) {
            prefsTemp.edit()
                    .putLong(Common.IMOD_LAST_UNLOCK_GLOBAL, System.currentTimeMillis())
                    .commit();
        }
        if (prefsIMod.getBoolean(Common.IMOD_DELAY_APP_ENABLED, false)) {
            prefsTemp.edit()
                    .putLong(packageName + Common.FLAG_IMOD, System.currentTimeMillis())
                    .commit();
        }
        openApp();
    }

    private void defaultSetup() {
        // Authentication fragment/UI
        setContentView(R.layout.activity_lock);
        Fragment frag = new LockFragment();
        Bundle b = new Bundle(1);
        b.putString(Common.INTENT_EXTRAS_PKG_NAME, packageName);
        frag.setArguments(b);
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();
    }

    private void fakeDieSetup() {
        PackageManager pm = getPackageManager();
        ApplicationInfo requestPkgInfo;
        try {
            requestPkgInfo = pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            requestPkgInfo = null;
        }

        String requestPkgFullName = (String) (requestPkgInfo != null ? pm.getApplicationLabel(requestPkgInfo) : "(unknown)");
        AlertDialog.Builder fakeDieDialogBuilder = new AlertDialog.Builder(this);
        fakeDieDialog = fakeDieDialogBuilder.setMessage(String.format(getResources().getString(R.string.dialog_text_fake_die_stopped), requestPkgFullName))
                .setNeutralButton(R.string.dialog_button_report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (prefs.getBoolean(Common.IMOD_MIN_FAKE_UNLOCK, false)) {
                            launchLockView(LockActivity.this, original, packageName, false);
                            finish();
                        } else {
                            final EditText input = new EditText(LockActivity.this);
                            input.setMinLines(3);
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                            fakeDieDialog.dismiss();
                            AlertDialog.Builder reportDialogBuilder = new AlertDialog.Builder(LockActivity.this);
                            reportDialog = reportDialogBuilder.setView(input)
                                    .setTitle(R.string.dialog_title_report_problem)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (input.getText().toString().equals(prefs.getString(Common.FAKE_DIE_INPUT, "start"))) {
                                                launchLockView(LockActivity.this, original, packageName, false);
                                            } else {
                                                Toast.makeText(LockActivity.this, "Thanks for your feedback", Toast.LENGTH_SHORT).show();
                                            }
                                            finish();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            finishAndBack();
                                        }
                                    })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialogInterface) {
                                            finishAndBack();
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
                        finishAndBack();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finishAndBack();
                    }
                })
                .create();
        fakeDieDialog.show();
    }

    @SuppressLint("CommitPrefEdits")
    private void finishAndBack() {
        prefsTemp.edit().putLong(packageName + Common.FLAG_CLOSE_APP, System.currentTimeMillis()).commit();
        finish();
    }

    private void openApp() {
        unlocked = true;
        directUnlock(this, original, packageName);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        isInFocus = hasFocus;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fakeDieDialog != null) {
            fakeDieDialog.dismiss();
        } else if (reportDialog != null) {
            reportDialog.dismiss();
        }
        if (!isInFocus) {
            Log.d("LockActivity", "Lost focus, finishing.");
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.ENABLE_LOGGING, false) && !unlocked) {
                Util.logFailedAuthentication(this, packageName);
            }
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAndBack();
    }
}
