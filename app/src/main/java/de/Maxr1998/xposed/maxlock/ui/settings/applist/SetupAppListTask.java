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

package de.Maxr1998.xposed.maxlock.ui.settings.applist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.Maxr1998.xposed.maxlock.BuildConfig;

class SetupAppListTask extends AsyncTask<Void, Integer, Void> {

    private final Context mContext;
    private final AppListFragment mFragment;
    private final List<ApplicationInfo> mAppList;
    private final AppListAdapter mAdapter;
    private ProgressDialog mProgressDialog;
    private List<ApplicationInfo> mAllApps;

    public SetupAppListTask(AppListFragment a, List<ApplicationInfo> list, AppListAdapter adapter) {
        mContext = a.getActivity();
        mFragment = a;
        mAppList = list;
        mAdapter = adapter;
    }

    @Override
    protected void onPreExecute() {
        mAllApps = mContext.getPackageManager().getInstalledApplications(0);
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mProgressDialog.dismiss();
                ((Activity) mContext).onBackPressed();
            }
        });
        mProgressDialog.setMax(mAllApps.size());
        mProgressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        mAppList.clear();
        PackageManager pm = mContext.getPackageManager();
        for (int i = 0; i < mAllApps.size(); i++) {
            ApplicationInfo ai = mAllApps.get(i);
            if ((pm.getLaunchIntentForPackage(ai.packageName) != null && !ai.packageName.equals(BuildConfig.APPLICATION_ID)) || ai.packageName.matches("com.(google.)?android.packageinstaller")) {
                mAppList.add(ai);
            }
            publishProgress(i + 1);
        }
        Collections.sort(mAppList, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                String s1 = lhs.loadLabel(mContext.getPackageManager()).toString();
                String s2 = rhs.loadLabel(mContext.getPackageManager()).toString();
                return s1.compareToIgnoreCase(s2);
            }
        });
        publishProgress(1);
        mAdapter.saveListBackup();
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mProgressDialog != null) {
            mProgressDialog.setProgress(progress[0]);
            if (progress[0] == mAllApps.size()) {
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setMax(1);
            }
        }
    }

    @Override
    protected void onPostExecute(Void v) {
        super.onPostExecute(v);
        mAdapter.notifyDataSetChanged();
        mFragment.filter();
        mProgressDialog.dismiss();
    }
}
