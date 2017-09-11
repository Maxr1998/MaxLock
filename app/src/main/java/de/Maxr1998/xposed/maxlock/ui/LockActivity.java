/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2016 Max Rumpf alias Maxr1998
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.MLImplementation;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.lockscreen.LockView;
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.NotificationHelper;
import de.Maxr1998.xposed.maxlock.util.Util;

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_HOME;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.appClosed;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.appUnlocked;

public class LockActivity extends AppCompatActivity implements AuthenticationSucceededListener {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private SharedPreferences prefs;
    @Keep
    private String[] names;
    private boolean unlocked = false;
    private AlertDialog fakeDieDialog, reportDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = MLPreferences.getPreferences(this);
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
        if (names == null) {
            names = new String[]{"", ""};
            Toast.makeText(this, "There was an error in the LockActivity, did you reboot after update?", Toast.LENGTH_LONG).show();
        }

        if (!fakeCrash) {
            defaultSetup();
        } else {
            fakeDieSetup();
        }
    }

    private void defaultSetup() {
        // Authentication fragment/UI
        setContentView(new LockView(LockView.getThemedContext(this), names[0], names[1]), new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
        fakeDieDialog = new AlertDialog.Builder(this)
                .setMessage(String.format(getResources().getString(R.string.dialog_text_fake_die_stopped), requestPkgFullName))
                .setNeutralButton(R.string.dialog_button_report, (dialogInterface, i) -> {
                    if (prefs.getBoolean(Common.ENABLE_DIRECT_UNLOCK, false)) {
                        fakeDieDialog.dismiss();
                        fakeDieDialog = null;
                        launchLockView();
                        finish();
                    } else {
                        fakeDieDialog.dismiss();
                        fakeDieDialog = null;
                        final EditText input = new EditText(LockActivity.this);
                        input.setMinLines(2);
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        reportDialog = new AlertDialog.Builder(LockActivity.this)
                                .setView(input)
                                .setTitle(R.string.dialog_title_report_problem)
                                .setPositiveButton(android.R.string.ok, (dialogInterface1, i1) -> {
                                    if (input.getText().toString().equals(prefs.getString(Common.FAKE_CRASH_INPUT, "start"))) {
                                        reportDialog.dismiss();
                                        reportDialog = null;
                                        launchLockView();
                                        finish();
                                    } else {
                                        Toast.makeText(LockActivity.this, "Thanks for your feedback", Toast.LENGTH_SHORT).show();
                                        onBackPressed();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, (dialogInterface12, i12) -> onBackPressed())
                                .setOnCancelListener(dialogInterface13 -> onBackPressed())
                                .create();
                        reportDialog.show();
                    }
                })
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> onBackPressed())
                .setOnCancelListener(dialogInterface -> onBackPressed())
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
        appUnlocked(names[0], MLPreferences.getPrefsHistory(this));
        unlocked = true;
        NotificationHelper.postIModNotification(this);
        finish();
        overridePendingTransition(0, R.anim.lockscreen_fade_out_fast);
    }

    @Override
    public void onBackPressed() {
        Log.d(Util.LOG_TAG_LOCKSCREEN, "Pressed back.");
        if (MLImplementation.getImplementation(prefs) == MLImplementation.DEFAULT) {
            appClosed(names[0], MLPreferences.getPrefsHistory(this));
        } else {
            Intent home = new Intent(ACTION_MAIN)
                    .addCategory(CATEGORY_HOME);
            startActivity(home);
        }
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        if (fakeDieDialog != null) {
            fakeDieDialog.dismiss();
        } else if (reportDialog != null) {
            reportDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fakeDieDialog != null) {
            fakeDieDialog.show();
        } else if (reportDialog != null) {
            reportDialog.show();
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