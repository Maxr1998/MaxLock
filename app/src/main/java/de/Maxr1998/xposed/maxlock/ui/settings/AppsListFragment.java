package de.Maxr1998.xposed.maxlock.ui.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.ui.settings.util.CheckBoxAdapter;


public class AppsListFragment extends Fragment {

    public ListView listView;
    List<Map<String, Object>> itemList;
    private CheckBoxAdapter mAdapter;
    private EditText search;
    private ViewGroup rootView;
    private SharedPreferences pref;
    private SetupAppList task;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_appslist, container, false);
        listView = (ListView) rootView.findViewById(R.id.listview);
        search = (EditText) rootView.findViewById(R.id.search);
        if (Util.noGingerbread())
            search.setAlpha(0);

        pref = getActivity().getSharedPreferences(Common.PREF, Activity.MODE_WORLD_READABLE);

        if (mAdapter == null || mAdapter.isEmpty()) {
            if (task == null)
                task = new SetupAppList();
            if (!task.getStatus().equals(AsyncTask.Status.RUNNING))
                task.execute();
        }

        return rootView;
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

    private class SetupAppList extends AsyncTask<Void, Integer, List> {

        ProgressDialog progressDialog;
        AsyncTask task = this;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(true);
            progressDialog.show();
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    task.cancel(true);
                }
            });
        }

        @Override
        protected List doInBackground(Void... voids) {
            Context activity = getActivity();

            PackageManager pm = activity.getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);

            progressDialog.setMax(list.size());

            itemList = new ArrayList<Map<String, Object>>();
            int i = 0;
            for (ApplicationInfo info : list) {
                if (isCancelled())
                    break;
                if (pref.getBoolean("show_system_apps", false) ?
                        activity.getPackageManager().getLaunchIntentForPackage(info.packageName) != null :
                        (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {

                    Map<String, Object> map = new HashMap<String, Object>();

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
        protected void onPostExecute(List list) {
            super.onPostExecute(list);
            progressDialog.dismiss();
            mAdapter = new CheckBoxAdapter(getActivity(), list);
            listView.setAdapter(mAdapter);
            if (Util.noGingerbread())
                search.setAlpha(1);
            setupSearch();
        }

        @Override
        protected void onCancelled() {
            getFragmentManager().popBackStack();
        }
    }
}
