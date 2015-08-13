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

package de.Maxr1998.xposed.maxlock.ui.settings.applist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.Maxr1998.xposed.maxlock.Common;

public class SetupAppListTask extends AsyncTask<Void, Integer, List<Map<String, Object>>> {

    private final Context mContext;
    public int mProgress = 0;
    private ProgressDialog mProgressDialog;
    private AppListFragment mFragment;
    private List<ApplicationInfo> mTempAppList;

    public SetupAppListTask(Context c, AppListFragment a) {
        mContext = c;
        mFragment = a;
    }

    @Override
    protected void onPreExecute() {
        mTempAppList = mContext.getPackageManager().getInstalledApplications(0);
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
        mProgressDialog.setMax(mTempAppList.size());
        mProgressDialog.show();
    }

    @Override
    protected List<Map<String, Object>> doInBackground(Void... voids) {
        List<Map<String, Object>> mItemList = new ArrayList<>();
        for (ApplicationInfo info : mTempAppList) {
            String pkgName = info.packageName;
            if ((mContext.getPackageManager().getLaunchIntentForPackage(pkgName) != null && !pkgName.equals(Common.PKG_NAME)) || pkgName.equals("com.android.packageinstaller")) {
                Map<String, Object> map = new HashMap<>();
                map.put("title", mContext.getPackageManager().getApplicationLabel(info));
                map.put("key", pkgName);
                try {
                    map.put("icon", mContext.getPackageManager().getApplicationIcon(info));
                } catch (OutOfMemoryError o) {
                    Log.e("MaxLock", "OutOfMemory while reading application icons!");
                }
                mItemList.add(map);
            }
            mProgress++;
            publishProgress(mProgress);
        }
        System.gc();
        Collections.sort(mItemList, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                String s1 = (String) lhs.get("title");
                String s2 = (String) rhs.get("title");
                return s1.compareToIgnoreCase(s2);
            }
        });
        return mItemList;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mProgressDialog != null)
            mProgressDialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(List<Map<String, Object>> list) {
        super.onPostExecute(list);
        mProgressDialog.dismiss();
        mFragment.appListFinished(list);
    }
}
