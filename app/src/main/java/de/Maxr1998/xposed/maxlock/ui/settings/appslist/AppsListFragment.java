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

package de.Maxr1998.xposed.maxlock.ui.settings.appslist;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.haibison.android.lockpattern.LockPatternActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;

public class AppsListFragment extends Fragment {

    List<Map<String, Object>> itemList, finalList;
    ViewGroup rootView;
    RecyclerView recyclerView;
    ProgressDialog progressDialog;
    AlertDialog restoreDialog;
    private AppListAdapter mAdapter;
    private SharedPreferences pref;
    private SetupAppList task;
    private ArrayAdapter<String> restoreAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        pref = getActivity().getSharedPreferences(Common.PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_appslist, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        if (finalList == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    task.cancel(true);
                }
            });
            progressDialog.show();
            if (task == null)
                task = new SetupAppList();
            if (!task.getStatus().equals(AsyncTask.Status.RUNNING))
                task.execute();
        } else {
            setup();
        }
        return rootView;
    }

    private void setup() {
        mAdapter = new AppListAdapter(AppsListFragment.this, getActivity(), finalList);
        recyclerView.setAdapter(mAdapter);
        if (progressDialog != null)
            progressDialog.dismiss();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.applist_menu, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.toolbar_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mAdapter.getFilter().filter(s);
                return true;
            }
        });
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (pref.getBoolean(Common.ENABLE_PRO, false)) {
            final File prefsPackagesFileShort = new File(Common.PREFS_PACKAGES + ".xml");
            final File prefsPerAppFileShort = new File(Common.PREFS_PER_APP + ".xml");
            final File prefsPackagesFile = new File(getActivity().getApplicationInfo().dataDir + File.separator + "shared_prefs" + File.separator + prefsPackagesFileShort);
            final File prefsPerAppFile = new File(getActivity().getApplicationInfo().dataDir + File.separator + "shared_prefs" + File.separator + prefsPerAppFileShort);
            final File backupDir = new File(Environment.getExternalStorageDirectory() + File.separator + "MaxLock_Backup");

            switch (item.getItemId()) {
                case R.id.toolbar_backup_list:
                    File curTimeDir = new File(backupDir + File.separator + new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())) + File.separator);
                    try {
                        if (prefsPackagesFile.exists()) {
                            FileUtils.copyFileToDirectory(prefsPackagesFile, curTimeDir);
                            if (prefsPerAppFile.exists())
                                FileUtils.copyFileToDirectory(prefsPerAppFile, curTimeDir);
                        } else
                            Toast.makeText(getActivity(), R.string.toast_no_files_to_backup, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(getActivity(), R.string.toast_backup_restore_exception, Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    if (curTimeDir.exists() && new File(curTimeDir + File.separator + prefsPackagesFileShort).exists())
                        Toast.makeText(getActivity(), R.string.toast_backup_success, Toast.LENGTH_SHORT).show();
                    return true;

                case R.id.toolbar_restore_list:
                    List<String> list = new ArrayList<>(Arrays.asList(backupDir.list()));
                    restoreAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, list);
                    restoreDialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.dialog_restore_list_message)
                            .setAdapter(restoreAdapter, new DialogInterface.OnClickListener() {
                                @SuppressLint("InlinedApi")
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    File restorePackagesFile = new File(backupDir + File.separator + restoreAdapter.getItem(i) + File.separator + prefsPackagesFileShort);
                                    File restorePerAppFile = new File(backupDir + File.separator + restoreAdapter.getItem(i) + File.separator + prefsPerAppFileShort);
                                    if (restorePackagesFile.exists()) {
                                        try {
                                            //noinspection ResultOfMethodCallIgnored
                                            prefsPackagesFile.delete();
                                            FileUtils.copyFile(restorePackagesFile, prefsPackagesFile);
                                            if (restorePerAppFile.exists()) {
                                                //noinspection ResultOfMethodCallIgnored
                                                prefsPerAppFile.delete();
                                                FileUtils.copyFile(restorePerAppFile, prefsPerAppFile);
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        getActivity().getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_MULTI_PROCESS);
                                        getActivity().getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_MULTI_PROCESS);
                                        Toast.makeText(getActivity(), R.string.toast_restore_success, Toast.LENGTH_SHORT).show();
                                        ((SettingsActivity) getActivity()).restart();
                                    } else
                                        Toast.makeText(getActivity(), R.string.toast_no_files_to_restore, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null).show();
                    restoreDialog.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                            try {
                                FileUtils.deleteDirectory(new File(backupDir + File.separator + restoreAdapter.getItem(i)));
                                restoreAdapter.remove(restoreAdapter.getItem(i));
                                restoreAdapter.notifyDataSetChanged();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return false;
                            }
                            return true;
                        }
                    });
                    return true;
                case R.id.toolbar_clear_list:
                    //noinspection deprecation
                    getActivity().getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE).edit().clear().commit();
                    //noinspection deprecation
                    getActivity().getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_WORLD_READABLE).edit().clear().commit();
                    ((SettingsActivity) getActivity()).restart();
            }
        } else
            Toast.makeText(getActivity(), R.string.toast_pro_required, Toast.LENGTH_SHORT).show();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (String.valueOf(requestCode).startsWith(String.valueOf(Util.PATTERN_CODE_APP))) {
            if (resultCode == LockPatternActivity.RESULT_OK) {
                String app = (String) finalList.get(Integer.parseInt(String.valueOf(requestCode).substring(1))).get("key");
                Util.receiveAndSetPattern(getActivity(), data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN), app);
            }
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    private class SetupAppList extends AsyncTask<Void, Integer, List<Map<String, Object>>> {
        @Override
        protected List<Map<String, Object>> doInBackground(Void... voids) {
            PackageManager pm = getActivity().getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);

            itemList = new ArrayList<>();
            int i = 0;
            for (ApplicationInfo info : list) {
                if (isCancelled())
                    break;
                progressDialog.setMax(list.size());
                if ((pref.getBoolean("show_system_apps", false) ?
                        getActivity().getPackageManager().getLaunchIntentForPackage(info.packageName) != null :
                        (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) && !info.packageName.equals(Common.PKG_NAME) || info.packageName.equals("com.android.packageinstaller")) {

                    Map<String, Object> map = new HashMap<>();

                    map.put("title", pm.getApplicationLabel(info));
                    map.put("key", info.packageName);
                    map.put("icon", pm.getApplicationIcon(info));

                    itemList.add(map);
                }
                i++;
                publishProgress(i);
            }

            Collections.sort(itemList, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                    String s1 = (String) lhs.get("title");
                    String s2 = (String) rhs.get("title");
                    return s1.compareToIgnoreCase(s2);
                }
            });
            return itemList;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> list) {
            super.onPostExecute(list);
            finalList = list;
            setup();
        }

        @Override
        protected void onCancelled() {
            getFragmentManager().popBackStack();
        }
    }
}