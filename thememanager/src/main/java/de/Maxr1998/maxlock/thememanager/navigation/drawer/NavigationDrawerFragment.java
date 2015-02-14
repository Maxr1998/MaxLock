/*
 * Theme Manager for MaxLock
 * Copyright (C) 2015  Maxr1998
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

package de.Maxr1998.maxlock.thememanager.navigation.drawer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.Maxr1998.maxlock.thememanager.Common;
import de.Maxr1998.maxlock.thememanager.R;
import de.Maxr1998.maxlock.thememanager.ThemeListFragment;
import de.Maxr1998.maxlock.thememanager.WikiFragment;

public class NavigationDrawerFragment extends Fragment implements View.OnClickListener {
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ThemeListFragment themeListFragment;
    private View drawerView;

    private boolean mFromSavedInstanceState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFromSavedInstanceState = savedInstanceState != null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        drawerView = inflater.inflate(R.layout.fragment_nav_drawer, container, false);

        final TextView version = (TextView) drawerView.findViewById(R.id.version);
        try {
            version.setText(getString(R.string.version) + " " + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        drawerView.findViewById(R.id.store).setOnClickListener(this);
        drawerView.findViewById(R.id.return_maxlock).setOnClickListener(this);
        return drawerView;
    }

    public void setUp(DrawerLayout drawerLayout, Toolbar toolbar) {
        mDrawerLayout = drawerLayout;
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, toolbar, R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerOpened(View drawerView) {
                if (themeListFragment != null) themeListFragment.hideBottomBar(true);
                super.onDrawerOpened(drawerView);
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(Common.USER_LEARNED_DRAWER, true).apply();
                //getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (themeListFragment != null) themeListFragment.reShowBottomBar();
                //getActivity().invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        if (!PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Common.USER_LEARNED_DRAWER, false) && !mFromSavedInstanceState)
            mDrawerLayout.openDrawer(getActivity().findViewById(R.id.scrimInsetsFrameLayout));

        final View[] drawerItems = {
                drawerView.findViewById(R.id.installed_themes),
                drawerView.findViewById(R.id.wiki)
        };

        for (View drawerItem : drawerItems) {
            drawerItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (View v : drawerItems) {
                        v.setBackgroundDrawable(getResources().getDrawable(R.drawable.selectable_item_background));
                        ((TextView) v).setTextColor(getResources().getColor(R.color.drawer_text_color));
                    }
                    displayView(view);
                }
            });
        }

        displayView(drawerItems[0]);
    }

    public void displayView(View view) {
        view.setBackground(getResources().getDrawable(R.color.background_selected));
        ((TextView) view).setTextColor(getResources().getColor(R.color.accent));
        Fragment fragment;
        switch (view.getId()) {
            case R.id.installed_themes:
                fragment = new ThemeListFragment();
                themeListFragment = (ThemeListFragment) fragment;
                break;
            case R.id.wiki:
                fragment = new WikiFragment();
                themeListFragment = null;
                break;
            default:
                throw new IllegalArgumentException("View is null or not a drawer item!");
        }
        getFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.store:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/search?q=pub:Maxr1998")));
                break;
            case R.id.return_maxlock:
                Intent maxlock = new Intent();
                maxlock.setComponent(new ComponentName("de.Maxr1998.xposed.maxlock", "de.Maxr1998.xposed.maxlock" + ".ui.SettingsActivity"));
                maxlock.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(maxlock);
                break;
        }
    }
}