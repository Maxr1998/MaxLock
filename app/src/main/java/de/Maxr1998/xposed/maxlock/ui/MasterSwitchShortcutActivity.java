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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;

@SuppressLint("CommitPrefEdits")
public class MasterSwitchShortcutActivity extends FragmentActivity implements AuthenticationSucceededListener {

    SharedPreferences prefsPackages;

    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection deprecation
        prefsPackages = getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE);

        if (prefsPackages.getBoolean(Common.MASTER_SWITCH_ON, true)) {
            setContentView(R.layout.activity_lock);
            Fragment frag = new LockFragment();
            Bundle b = new Bundle(1);
            b.putString(Common.INTENT_EXTRAS_PKG_NAME, getString(R.string.unlock_master_switch));
            frag.setArguments(b);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).commit();

        } else {
            prefsPackages.edit().putBoolean(Common.MASTER_SWITCH_ON, true).commit();
            Toast.makeText(this, getString(R.string.toast_master_switch_on), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onAuthenticationSucceeded() {
        prefsPackages.edit().putBoolean(Common.MASTER_SWITCH_ON, false).commit();
        Toast.makeText(this, getString(R.string.toast_master_switch_off), Toast.LENGTH_LONG).show();
        finish();
    }
}