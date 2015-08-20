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

package de.Maxr1998.xposed.maxlock.ui.settings.lockingtype;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.settings.SettingsFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.WebsiteFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListFragment;

public abstract class MaxLockPreferenceFragment extends PreferenceFragment {

    protected SharedPreferences prefs;
    protected String title = null;

    public static void launchFragment(@NonNull Fragment fragment, boolean fromRoot, @NonNull Fragment from) {
        if (fromRoot) {
            from.getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        from.getFragmentManager().beginTransaction().replace(R.id.frame_container, fragment, fragment instanceof AppListFragment ? "AppListFragment" : fragment instanceof WebsiteFragment ? "WebsiteFragment" : null).addToBackStack(null).commit();
        if (from.getFragmentManager().findFragmentById(R.id.settings_fragment) != null)
            from.getFragmentManager().beginTransaction().show(from.getFragmentManager().findFragmentById(R.id.settings_fragment)).commit();
    }

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        //noinspection deprecation
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        getActivity().setTitle(title);
        if (!(this instanceof SettingsFragment)) {
            //noinspection ConstantConditions
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        return super.onCreateView(paramLayoutInflater, paramViewGroup, paramBundle);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getListView().setPadding(0, 0, 0, 0);
        //noinspection deprecation
        getListView().setOverscrollFooter(new ColorDrawable(getListView().getContext().getResources().getColor(
                !prefs.getBoolean(Common.USE_DARK_STYLE, false) ? R.color.default_window_background : R.color.default_window_background_dark)));
    }
}
