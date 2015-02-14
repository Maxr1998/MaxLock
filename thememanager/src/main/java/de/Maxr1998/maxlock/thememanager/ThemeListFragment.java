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

package de.Maxr1998.maxlock.thememanager;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemeListFragment extends Fragment implements View.OnClickListener {

    private ViewGroup rootView;
    RecyclerView mRecyclerView;
    ThemeAdapter mAdapter;
    View mBottomBar;
    TextView mTvInstall;
    Button mButtonInstall;
    private int position = -1;
    private boolean mShown = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new LoadPackages().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_theme_list, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                    hideBottomBar(false);
            }
        });
        mBottomBar = rootView.findViewById(R.id.bottom_bar);
        mBottomBar.setOnClickListener(this);
        mTvInstall = (TextView) rootView.findViewById(R.id.text_install);
        mButtonInstall = (Button) rootView.findViewById(R.id.button_install);
        mButtonInstall.setOnClickListener(this);

        return rootView;
    }

    public void showBottomBar(int position) {
        this.position = position;
        mTvInstall.setText(String.format(getString(R.string.install_theme), mAdapter.getItem(position).get("title")));
        mBottomBar.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_bar_show));
        mBottomBar.setVisibility(View.VISIBLE);
        mShown = true;
    }

    public void hideBottomBar(boolean temporary) {
        if (mBottomBar.isShown()) {
            mBottomBar.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_bar_hide));
            mBottomBar.setVisibility(View.GONE);
        }
        if (!temporary) {
            position = -1;
            mShown = false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_install:
                if (position != -1) {
                    hideBottomBar(true);
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("de.Maxr1998.xposed.maxlock", "de.Maxr1998.xposed.maxlock.ui.ThemeService"));
                    intent.putExtra("extra", 1);
                    intent.putExtra("package", mAdapter.getItem(position).get("packageName"));
                    getActivity().startService(intent);
                    SnackbarManager.show(Snackbar.with(getActivity()).text(R.string.theme_installed).actionLabel(R.string.undo).actionListener(new ActionClickListener() {
                        @Override
                        public void onActionClicked(Snackbar snackbar) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("de.Maxr1998.xposed.maxlock", "de.Maxr1998.xposed.maxlock.ui.ThemeService"));
                            intent.putExtra("extra", 2);
                            getActivity().startService(intent);
                            SnackbarManager.show(Snackbar.with(getActivity()).text(R.string.theme_uninstalled).duration(Snackbar.SnackbarDuration.LENGTH_SHORT));
                        }
                    }).actionColorResource(R.color.accent));
                    hideBottomBar(false);
                }
                break;
        }
    }

    public void reShowBottomBar() {
        if (mShown) {
            mBottomBar.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_bar_show));
            mBottomBar.setVisibility(View.VISIBLE);
        }
    }

    public class LoadPackages extends AsyncTask<Void, Void, List<Map<String, String>>> {

        @Override
        protected List<Map<String, String>> doInBackground(Void... voids) {
            List<Map<String, String>> themes = new ArrayList<>();
            PackageManager pm = getActivity().getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo info : list) {
                if (info.metaData != null && info.metaData.containsKey("maxlock_theme") && info.metaData.getBoolean("maxlock_theme", false)) {
                    Map<String, String> map = new HashMap<>();
                    map.put("title", (String) pm.getApplicationLabel(info));
                    map.put("packageName", info.packageName);
                    try {
                        map.put("version", pm.getPackageInfo(info.packageName, 0).versionName + " (" + info.metaData.getFloat("maxlock_version") + ")");
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    themes.add(map);
                }
            }
            return themes;
        }

        @Override
        protected void onPostExecute(List<Map<String, String>> result) {
            rootView.findViewById(R.id.spinner).setVisibility(View.GONE);
            mAdapter = new ThemeAdapter(ThemeListFragment.this, getActivity(), result);
            mRecyclerView.setAdapter(mAdapter);
            if (mAdapter.getItemCount() == 0) rootView.findViewById(R.id.no_themes_text).setVisibility(View.VISIBLE);
        }
    }
}
