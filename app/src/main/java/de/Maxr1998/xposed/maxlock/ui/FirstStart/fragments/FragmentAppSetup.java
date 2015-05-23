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

package de.Maxr1998.xposed.maxlock.ui.FirstStart.fragments;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.settings.SettingsFragment;

public class FragmentAppSetup extends Fragment implements CompoundButton.OnCheckedChangeListener {

    ViewGroup rootView;
    DevicePolicyManager devicePolicyManager;
    ComponentName deviceAdmin;
    SharedPreferences prefsPackages;
    private String[] app_names = {
            "com.android.packageinstaller", "com.android.settings", "de.robv.android.xposed.installer"
    };

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_first_start_setup, container, false);
        prefsPackages = getActivity().getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE);
        devicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(getActivity(), SettingsFragment.UninstallProtectionReceiver.class);
        CheckBox[] app_cbs = {
                (CheckBox) rootView.findViewById(R.id.first_start_app_package),
                (CheckBox) rootView.findViewById(R.id.first_start_app_settings),
                (CheckBox) rootView.findViewById(R.id.first_start_app_xposed),
                (CheckBox) rootView.findViewById(R.id.first_start_app_device_admin)
        };
        for (CheckBox cb : app_cbs) {
            cb.setOnCheckedChangeListener(this);
        }
        for (int android = 0; android < 3; android++) {
            app_cbs[android].setChecked(prefsPackages.getBoolean(app_names[android], false));
        }
        app_cbs[3].setChecked(devicePolicyManager.isAdminActive(deviceAdmin));
        return rootView;
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
                prefsPackages.edit().putBoolean(app_names[c], b).commit();
                break;
            case R.id.first_start_app_device_admin:
                if (b) {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
                    startActivity(intent);
                } else {
                    devicePolicyManager.removeActiveAdmin(deviceAdmin);
                }
                break;
        }
    }
}
