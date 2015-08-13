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
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
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
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;
import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

public class AppListFragment extends Fragment {

    private static List<Map<String, Object>> APP_LIST;
    private static SetupAppList TASK;
    private static ProgressDialog PROGRESS_DIALOG;
    RecyclerView recyclerView;
    AlertDialog restoreDialog;
    VerticalRecyclerViewFastScroller fastScroller;
    SectionTitleIndicator scrollIndicator;
    private AppListAdapter mAdapter;
    private SharedPreferences prefs;
    private ArrayAdapter<String> restoreAdapter;

    public static void clearList() {
        APP_LIST = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        prefs = getActivity().getSharedPreferences(Common.PREFS, Context.MODE_PRIVATE);
        mAdapter = new AppListAdapter(AppListFragment.this, getActivity());
        // Generate list
        if (APP_LIST == null && TASK == null) {
            TASK = new SetupAppList();
            TASK.execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_appslist, container, false);
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
        if (APP_LIST == null) {
            PROGRESS_DIALOG = new ProgressDialog(getActivity());
            PROGRESS_DIALOG.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            PROGRESS_DIALOG.setCanceledOnTouchOutside(false);
            PROGRESS_DIALOG.setCancelable(true);
            PROGRESS_DIALOG.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    PROGRESS_DIALOG.dismiss();
                    getActivity().onBackPressed();
                }
            });
            PROGRESS_DIALOG.setMax(TASK.max);
            PROGRESS_DIALOG.show();
        } else {
            appListFinished();
        }
        return rootView;
    }

    private void appListFinished() {
        mAdapter.updateList(APP_LIST);
        filter();
        if (PROGRESS_DIALOG != null)
            PROGRESS_DIALOG.dismiss();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.applist_menu, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.toolbar_search));
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.findItem(R.id.toolbar_filter_activated).setVisible(false);
            }
        });
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
            final String prefsAppsName = Common.PREFS_APPS + ".xml";
            final String prefsPerAppName = Common.PREFS_KEYS_PER_APP + ".xml";
            final File prefsAppsFile = new File(Util.dataDir(getActivity()) + "shared_prefs/" + prefsAppsName);
            final File prefsPerAppFile = new File(Util.dataDir(getActivity()) + "shared_prefs/" + prefsPerAppName);

            switch (item.getItemId()) {
                case R.id.toolbar_backup_list:
                    String currentBackupDirPath = Common.BACKUP_DIR + new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss", Locale.getDefault())
                            .format(new Date(System.currentTimeMillis())) + File.separator;
                    backupFile(prefsAppsFile, new File(currentBackupDirPath));
                    backupFile(prefsPerAppFile, new File(currentBackupDirPath));
                    if (new File(currentBackupDirPath).exists() && new File(currentBackupDirPath + prefsAppsName).exists())
                        Toast.makeText(getActivity(), R.string.toast_backup_success, Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.toolbar_restore_list:
                    List<String> list = new ArrayList<>(Arrays.asList(new File(Common.BACKUP_DIR).list()));
                    restoreAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, list);
                    restoreDialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.dialog_text_restore_list)
                            .setAdapter(restoreAdapter, new DialogInterface.OnClickListener() {
                                @SuppressLint("InlinedApi")
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    File restorePackagesFile = new File(Common.BACKUP_DIR + restoreAdapter.getItem(i) + File.separator + prefsAppsName);
                                    File restorePerAppFile = new File(Common.BACKUP_DIR + restoreAdapter.getItem(i) + File.separator + prefsPerAppName);
                                    try {
                                        if (restorePackagesFile.exists()) {
                                            //noinspection ResultOfMethodCallIgnored
                                            prefsAppsFile.delete();
                                            FileUtils.copyFile(restorePackagesFile, prefsAppsFile);
                                        }
                                        if (restorePerAppFile.exists()) {
                                            //noinspection ResultOfMethodCallIgnored
                                            prefsPerAppFile.delete();
                                            FileUtils.copyFile(restorePerAppFile, prefsPerAppFile);
                                        }
                                    } catch (IOException e) {
                                        Toast.makeText(getActivity(), R.string.toast_no_files_to_restore, Toast.LENGTH_SHORT).show();
                                    }
                                    getActivity().getSharedPreferences(Common.PREFS_APPS, Context.MODE_MULTI_PROCESS);
                                    getActivity().getSharedPreferences(Common.PREFS_KEYS_PER_APP, Context.MODE_MULTI_PROCESS);
                                    Toast.makeText(getActivity(), R.string.toast_restore_success, Toast.LENGTH_SHORT).show();
                                    ((SettingsActivity) getActivity()).restart();
                                }
                            }).setNegativeButton(android.R.string.cancel, null).show();
                    restoreDialog.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                            try {
                                FileUtils.deleteDirectory(new File(Common.BACKUP_DIR + restoreAdapter.getItem(i)));
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
                    MLPreferences.getPrefsApps(getActivity()).edit().clear().commit();
                    getActivity().getSharedPreferences(Common.PREFS_KEYS_PER_APP, Context.MODE_PRIVATE).edit().clear().commit();
                    ((SettingsActivity) getActivity()).restart();
                    return true;
                case R.id.toolbar_filter_activated:
                    String appListFilter = prefs.getString("app_list_filter", "");
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

    @SuppressWarnings("deprecation")
    private void filterIcon(MenuItem item) {
        if (prefs == null) {
            return;
        }
        String filter = prefs.getString("app_list_filter", "");
        Drawable icon = getResources().getDrawable(R.drawable.ic_apps_white_24dp);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (String.valueOf(requestCode).startsWith(String.valueOf(Util.PATTERN_CODE_APP))) {
            if (resultCode == LockPatternActivity.RESULT_OK) {
                String app = (String) APP_LIST.get(Integer.parseInt(String.valueOf(requestCode).substring(1))).get("key");
                Util.receiveAndSetPattern(getActivity(), data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN), app);
            }
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    private class SetupAppList extends AsyncTask<Void, Integer, List<Map<String, Object>>> {

        public int max = 0;
        public int progress = 0;
        private List<Map<String, Object>> itemList;

        @Override
        protected List<Map<String, Object>> doInBackground(Void... voids) {
            System.gc();
            PackageManager pm = getActivity().getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);
            max = list.size();
            itemList = new ArrayList<>();
            for (ApplicationInfo info : list) {
                while (getActivity() == null) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String pkgName = info.packageName;
                if ((getActivity().getPackageManager().getLaunchIntentForPackage(pkgName) != null && !pkgName.equals(Common.PKG_NAME)) || pkgName.equals("com.android.packageinstaller")) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("title", pm.getApplicationLabel(info));
                    map.put("key", pkgName);
                    try {
                        map.put("icon", pm.getApplicationIcon(info));
                    } catch (OutOfMemoryError o) {
                        Log.e("MaxLock", "OutOfMemory while reading application icons!");
                    }
                    itemList.add(map);
                }
                progress++;
                publishProgress(progress);
            }
            System.gc();
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
            if (PROGRESS_DIALOG != null)
                PROGRESS_DIALOG.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> list) {
            super.onPostExecute(list);
            APP_LIST = list;
            appListFinished();
        }
    }
}