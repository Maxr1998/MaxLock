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
import android.text.InputType;
import android.widget.EditText;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.LockHelper;
import de.Maxr1998.xposed.maxlock.R;

import static de.Maxr1998.xposed.maxlock.LockHelper.launchLockView;


public class FakeDieDialog extends Activity {

    private String packageName;
    private Intent original;
    private AlertDialog.Builder reportDialog;
    private SharedPreferences prefs;

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Intent Extras
        packageName = getIntent().getStringExtra(Common.INTENT_EXTRAS_PKG_NAME);
        original = getIntent().getParcelableExtra(Common.INTENT_EXTRAS_INTENT);

        // Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences prefsPackages = getSharedPreferences(Common.PREFS_PACKAGES, MODE_WORLD_READABLE);
        SharedPreferences prefsIMoD = getSharedPreferences(Common.PREFS_IMOD, MODE_WORLD_READABLE);
        SharedPreferences prefsIMoDTemp = getSharedPreferences(Common.PREFS_IMOD_TEMP, MODE_WORLD_READABLE);

        long permitTimestamp = prefsPackages.getLong(packageName + "_tmp", 0);
        if (LockHelper.timerOrIMod(packageName, permitTimestamp, prefsIMoD, prefsIMoDTemp)) {
            LockActivity.directUnlock(this, original, packageName);
            finish();
            return;
        }

        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        PackageManager pm = getPackageManager();
        ApplicationInfo requestPkgInfo;
        try {
            requestPkgInfo = pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            requestPkgInfo = null;
        }

        String requestPkgFullName = (String) (requestPkgInfo != null ? pm.getApplicationLabel(requestPkgInfo) : "(unknown)");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(String.format(getResources().getString(R.string.dialog_text_fake_die_stopped), requestPkgFullName))
                .setNeutralButton(R.string.dialog_button_report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (prefs.getBoolean(Common.IMOD_MIN_FAKE_UNLOCK, false)) {
                            launchLockView(FakeDieDialog.this, original, packageName, ".ui.LockActivity");
                        } else {
                            final EditText input = new EditText(FakeDieDialog.this);
                            input.setMinLines(3);
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                            reportDialog = new AlertDialog.Builder(FakeDieDialog.this);
                            reportDialog.setView(input)
                                    .setTitle(R.string.dialog_title_report_problem)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (input.getText().toString().equals(prefs.getString(Common.FAKE_DIE_INPUT, "start"))) {
                                                launchLockView(FakeDieDialog.this, original, packageName, ".ui.LockActivity");
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
