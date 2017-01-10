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

package de.Maxr1998.xposed.maxlock.ui.firstStart.views;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;

public class ConfigView extends LinearLayout implements CompoundButton.OnCheckedChangeListener {

    private final String[] app_names = {getPackageInstallerID(), "com.android.settings", "de.robv.android.xposed.installer"};
    private final DevicePolicyManager devicePolicyManager = isInEditMode() ? null : (DevicePolicyManager) getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
    private final ComponentName deviceAdmin = isInEditMode() ? null : new ComponentName(getContext(), SettingsActivity.UninstallProtectionReceiver.class);

    public ConfigView(Context context) {
        this(context, null);
    }

    public ConfigView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConfigView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode())
            return;
        CheckBox[] app_cbs = {
                (CheckBox) findViewById(R.id.first_start_app_package),
                (CheckBox) findViewById(R.id.first_start_app_settings),
                (CheckBox) findViewById(R.id.first_start_app_xposed),
                (CheckBox) findViewById(R.id.first_start_app_device_admin)
        };
        for (int i = 0; i < 3; i++) {
            app_cbs[i].setChecked(MLPreferences.getPrefsApps(getContext()).getBoolean(app_names[i], false));
        }
        app_cbs[3].setChecked(devicePolicyManager.isAdminActive(deviceAdmin));
        for (CheckBox cb : app_cbs) {
            cb.setOnCheckedChangeListener(this);
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        int c = -1;
        switch (compoundButton.getId()) {
            case R.id.first_start_app_xposed:
                c++;
            case R.id.first_start_app_settings:
                c++;
            case R.id.first_start_app_package:
                c++;
                MLPreferences.getPrefsApps(getContext()).edit().putBoolean(app_names[c], b).commit();
                break;
            case R.id.first_start_app_device_admin:
                if (b) {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
                    getContext().startActivity(intent);
                } else {
                    devicePolicyManager.removeActiveAdmin(deviceAdmin);
                }
                break;
        }
    }

    private String getPackageInstallerID() {
        try {
            if (isInEditMode())
                return "";
            return getContext().getPackageManager().getPackageInfo("com.google.android.packageinstaller", 0).packageName;
        } catch (PackageManager.NameNotFoundException e) {
            return "com.android.packageinstaller";
        }
    }
}