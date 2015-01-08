package de.Maxr1998.xposed.maxlock.ui.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.util.CheckBoxAdapter;


public class AppsListFragment extends Fragment {

    List<Map<String, Object>> itemList, finalList;
    ViewGroup rootView;
    ListView listView;
    ProgressDialog progressDialog;
    private CheckBoxAdapter mAdapter;
    private EditText search;
    private SharedPreferences pref;
    private SetupAppList task;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        pref = getActivity().getSharedPreferences(Common.PREFS, Context.MODE_PRIVATE);
    }

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_appslist, container, false);
        listView = (ListView) rootView.findViewById(R.id.listview);
        search = (EditText) rootView.findViewById(R.id.search);
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
            if (Util.noGingerbread())
                search.setAlpha(0);
            if (task == null)
                task = new SetupAppList();
            if (!task.getStatus().equals(AsyncTask.Status.RUNNING))
                task.execute();
        } else {
            setup();
        }
        return rootView;
    }

    @SuppressLint("NewApi")
    private void setup() {
        mAdapter = new CheckBoxAdapter(getActivity(), finalList);
        listView.setAdapter(mAdapter);
        setupSearch();
        if (Util.noGingerbread())
            search.setAlpha(1);
        if (progressDialog != null)
            progressDialog.dismiss();
    }

    private void setupSearch() {
        search.clearFocus();
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAdapter.getFilter().filter(s);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.applist_menu, menu);
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
                case R.id.backup_list:
                    File curTimeDir = new File(backupDir + File.separator + new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date(System.currentTimeMillis())) + File.separator);
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

                case R.id.restore_list:
                    final ListAdapter la = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, backupDir.list());
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.dialog_restore_list_message)
                            .setAdapter(la, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    File restorePackagesFile = new File(backupDir + File.separator + la.getItem(i) + File.separator + prefsPackagesFileShort);
                                    File restorePerAppFile = new File(backupDir + File.separator + la.getItem(i) + File.separator + prefsPerAppFileShort);
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
                                        ((SettingsActivity) getActivity()).restart();
                                    } else
                                        Toast.makeText(getActivity(), R.string.toast_no_files_to_backup, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .create().show();
                    return true;
                case R.id.clear_list:
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

    private class SetupAppList extends AsyncTask<Void, Integer, List<Map<String, Object>>> {

        @Override
        protected List<Map<String, Object>> doInBackground(Void... voids) {
            Context activity = getActivity();

            PackageManager pm = activity.getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);

            itemList = new ArrayList<>();
            int i = 0;
            for (ApplicationInfo info : list) {
                if (isCancelled())
                    break;
                progressDialog.setMax(list.size());
                if (pref.getBoolean("show_system_apps", false) ?
                        activity.getPackageManager().getLaunchIntentForPackage(info.packageName) != null :
                        (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {

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

        @SuppressLint("NewApi")
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
