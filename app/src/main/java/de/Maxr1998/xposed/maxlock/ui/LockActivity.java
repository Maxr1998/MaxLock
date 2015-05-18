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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.LockHelper;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class LockActivity extends FragmentActivity implements AuthenticationSucceededListener {

    private String packageName;
    private Intent original;
    private SharedPreferences prefsPackages, prefsIMod, prefsIModTemp;
    private boolean isInFocus = false, unlocked = false;

    @SuppressLint("WorldReadableFiles")
    public static void directUnlock(Activity caller, Intent orig, String pkgName) {
        try {
            //noinspection deprecation
            caller.getSharedPreferences(Common.PREFS_PACKAGES, MODE_WORLD_READABLE).edit()
                    .putLong(pkgName + "_tmp", System.currentTimeMillis())
                    .commit();
            orig.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            caller.startActivity(orig);
        } catch (Exception e) {
            Intent intent_option = caller.getPackageManager().getLaunchIntentForPackage(pkgName);
            intent_option.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            caller.startActivity(intent_option);
        } finally {
            caller.finish();
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Intent extras
        packageName = getIntent().getStringExtra(Common.INTENT_EXTRAS_PKG_NAME);
        original = getIntent().getParcelableExtra(Common.INTENT_EXTRAS_INTENT);

        // Preferences
        prefsPackages = getSharedPreferences(Common.PREFS_PACKAGES, MODE_WORLD_READABLE);
        prefsIMod = getSharedPreferences(Common.PREFS_IMOD, MODE_WORLD_READABLE);
        prefsIModTemp = getSharedPreferences(Common.PREFS_IMOD_TEMP, MODE_WORLD_READABLE);

        long permitTimestamp = prefsPackages.getLong(packageName + "_tmp", 0);
        if (LockHelper.timerOrIMod(packageName, permitTimestamp, prefsIMod, prefsIModTemp)) {
            openApp();
            return;
        }

        // Authentication fragment/UI
        setContentView(R.layout.activity_lock);
        Fragment frag = new LockFragment();
        Bundle b = new Bundle(1);
        b.putString(Common.INTENT_EXTRAS_PKG_NAME, packageName);
        frag.setArguments(b);
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();
        ((ThisApplication) getApplication()).getTracker(ThisApplication.TrackerName.APP_TRACKER);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onAuthenticationSucceeded() {
        // Save time for Intika mod
        prefsIModTemp.edit()
                .putLong(packageName + "_imod", System.currentTimeMillis())
                .commit();
        // Clean Up
        prefsPackages.edit()
                .remove(packageName + "_imod")
                .commit();
        prefsIModTemp.edit()
                .putLong(Common.IMOD_LAST_UNLOCK_GLOBAL, System.currentTimeMillis())
                .commit();
        openApp();
    }

    @SuppressLint("CommitPrefEdits")
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.ENABLE_LOGGING, false) && !unlocked) {
            Util.logFailedAuthentication(this, packageName);
        }
        if (!isInFocus) {
            Log.d("MaxLock/LockActivity", "Lost focus, finishing.");
            finish();
        }
    }
}
