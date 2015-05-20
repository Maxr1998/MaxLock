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
import android.graphics.drawable.Drawable;
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
import java.io.FileNotFoundException;
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
import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

public class AppListFragment extends Fragment {

    private static List<Map<String, Object>> finalList;
    RecyclerView recyclerView;
    AlertDialog restoreDialog;
    VerticalRecyclerViewFastScroller fastScroller;
    SectionTitleIndicator scrollIndicator;
    private ViewGroup rootView;
    private AppListAdapter mAdapter;
    private SharedPreferences prefs;
    private SetupAppList task;
    private ArrayAdapter<String> restoreAdapter;
    private ProgressDialog progressDialog;

    public static void clearList() {
        finalList = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        prefs = getActivity().getSharedPreferences(Common.PREFS, Context.MODE_PRIVATE);
        mAdapter = new AppListAdapter(AppListFragment.this, getActivity());
        // Generate list
        if (finalList == null || finalList.isEmpty()) {
            task = new SetupAppList();
            task.execute();
        } else {
            appListFinished();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_appslist, container, false);
        // Setup layout
        recyclerView = (RecyclerView) rootView.findViewById(R.id.app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        fastScroller = (VerticalRecyclerViewFastScroller) rootView.findViewById(R.id.fast_scroller);
        fastScroller.setRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(fastScroller.getOnScrollListener());
        scrollIndicator = (SectionTitleIndicator) rootView.findViewById(R.id.fast_scroller_section_title_indicator);
        fastScroller.setSectionIndicator(scrollIndicator);

        // Show progress dialog
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
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
        if (task != null && task.getStatus().equals(AsyncTask.Status.RUNNING)) {
            progressDialog.show();
            progressDialog.setMax(task.listSize);
        }
        return rootView;
    }

    private void appListFinished() {
        mAdapter.updateList(finalList);
        filter();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
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
        filterIcon(menu.findItem(R.id.toolbar_filter_activated));
    }

    @SuppressLint({"WorldReadableFiles", "CommitPrefEdits"})
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (prefs.getBoolean(Common.ENABLE_PRO, false) || item.getItemId() == R.id.toolbar_filter_activated) {
            final File prefsPackagesFileShort = new File(Common.PREFS_PACKAGES + ".xml");
            final File prefsPerAppFileShort = new File(Common.PREFS_PER_APP + ".xml");
            final File prefsActivitiesFileShort = new File(Common.PREFS_ACTIVITIES + ".xml");
            final File prefsPackagesFile = new File(getActivity().getApplicationInfo().dataDir + File.separator + "shared_prefs" + File.separator + prefsPackagesFileShort);
            final File prefsPerAppFile = new File(getActivity().getApplicationInfo().dataDir + File.separator + "shared_prefs" + File.separator + prefsPerAppFileShort);
            final File prefsActivitiesFile = new File(getActivity().getApplicationInfo().dataDir + File.separator + "shared_prefs" + File.separator + prefsActivitiesFileShort);
            final File backupDir = new File(Environment.getExternalStorageDirectory() + File.separator + "MaxLock_Backup");

            switch (item.getItemId()) {
                case R.id.toolbar_backup_list:
                    File curTimeDir = new File(backupDir + File.separator + new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())) + File.separator);
                    backupFile(prefsPackagesFile, curTimeDir);
                    backupFile(prefsPerAppFile, curTimeDir);
                    backupFile(prefsActivitiesFile, curTimeDir);
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
                                    File restoreActivitiesFile = new File(backupDir + File.separator + restoreAdapter.getItem(i) + File.separator + prefsActivitiesFileShort);
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
                                            if (prefsActivitiesFile.exists()) {
                                                //noinspection ResultOfMethodCallIgnored
                                                prefsActivitiesFile.delete();
                                                FileUtils.copyFile(restoreActivitiesFile, prefsActivitiesFile);
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
                    getActivity().getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_PRIVATE).edit().clear().commit();
                    ((SettingsActivity) getActivity()).restart();
                    return true;
                case R.id.toolbar_filter_activated:
                    String appListFilter = prefs.getString("app_list_filter", "");
                    if (appListFilter == null) {
                        return true;
                    }
                    switch (appListFilter) {
                        case "@*activated*":
                            prefs.edit().putString("app_list_filter", "@*deactivated*").commit();
                            break;
                        case "@*deactivated*":
                            prefs.edit().putString("app_list_filter", "").commit();
                            break;
                        default:
                            prefs.edit().putString("app_list_filter", "@*activated*").commit();
                            break;
                    }
                    filterIcon(item);
                    filter();
                    return true;
            }
        } else
            Toast.makeText(getActivity(), R.string.toast_pro_required, Toast.LENGTH_SHORT).show();
        return super.onOptionsItemSelected(item);
    }

    private void filterIcon(MenuItem item) {
        if (prefs == null) {
            return;
        }
        String filter = prefs.getString("app_list_filter", "");
        Drawable icon = getResources().getDrawable(R.drawable.ic_apps_white_24dp);
        if (filter == null) {
            return;
        }
        switch (filter) {
            case "@*activated*":
                icon = getResources().getDrawable(R.drawable.ic_check_white_24dp);
                break;
            case "@*deactivated*":
                icon = getResources().getDrawable(R.drawable.ic_close_white_24dp);
                break;
        }
        item.setIcon(icon);
    }

    private void filter() {
        mAdapter.getFilter().filter(prefs.getString("app_list_filter", ""));
    }

    public void backupFile(File file, File directory) {
        try {
            FileUtils.copyFileToDirectory(file, directory);
        } catch (FileNotFoundException f) {
            f.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(getActivity(), R.string.toast_backup_restore_exception, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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

        public int listSize = 0;
        private List<Map<String, Object>> itemList;

        @Override
        protected List<Map<String, Object>> doInBackground(Void... voids) {
            PackageManager pm = getActivity().getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);
            listSize = list.size();

            itemList = new ArrayList<>();
            int i = 0;
            for (ApplicationInfo info : list) {
                if (isCancelled())
                    break;
                while (getActivity() == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if ((prefs.getBoolean("show_system_apps", false) ?
                        getActivity().getPackageManager().getLaunchIntentForPackage(info.packageName) != null :
                        (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) && !info.packageName.equals(Common.PKG_NAME) || info.packageName.equals("com.android.packageinstaller")) {

                    Map<String, Object> map = new HashMap<>();

                    map.put("title", pm.getApplicationLabel(info));
                    map.put("key", info.packageName);
                    map.put("icon", pm.getApplicationIcon(info));

                    itemList.add(map);
                }
                publishProgress(i);
                i++;
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
            if (progressDialog != null) progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> list) {
            super.onPostExecute(list);
            finalList = list;
            appListFinished();
        }

        @Override
        protected void onCancelled() {
            getFragmentManager().popBackStack();
        }
    }
}