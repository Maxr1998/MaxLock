/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2018  Max Rumpf alias Maxr1998
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

package de.Maxr1998.xposed.maxlock.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;

public class LogViewerFragment extends Fragment {

    private RecyclerView mLogRecycler;
    private TextView mEmptyText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getActivity().setTitle(getString(R.string.pref_screen_logs));
        View rootView = inflater.inflate(R.layout.fragment_logs, container, false);
        mLogRecycler = rootView.findViewById(R.id.log_recycler);
        mEmptyText = rootView.findViewById(R.id.logs_empty_text);
        List<String> text = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(getActivity().getApplicationInfo().dataDir + File.separator + Common.LOG_FILE));
            String line;
            while ((line = br.readLine()) != null) {
                text.add(line);
            }
        } catch (FileNotFoundException e) {
            mLogRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            return rootView;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mLogRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mLogRecycler.setItemAnimator(new DefaultItemAnimator());
        LogRecyclerAdapter adapter = new LogRecyclerAdapter(text);
        mLogRecycler.setAdapter(adapter);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(getString(R.string.pref_screen_logs));
        //noinspection ConstantConditions
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.logviewer_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.toolbar_delete_log) {
            File file = new File(getActivity().getApplicationInfo().dataDir + File.separator + Common.LOG_FILE);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            mLogRecycler.setVisibility(View.GONE);
            mEmptyText.findViewById(R.id.logs_empty_text).setVisibility(View.VISIBLE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class LogRecyclerAdapter extends RecyclerView.Adapter<LogRecyclerAdapter.LogViewHolder> {

        private List<String> data;

        public LogRecyclerAdapter(@NonNull List<String> d) {
            data = d;
        }

        @Override
        public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_log_item, parent, false);
            return new LogViewHolder(v);
        }

        @Override
        public void onBindViewHolder(LogViewHolder holder, int p) {
            int position = holder.getLayoutPosition();
            String mCurrent = data.get(position);
            boolean showDate;
            if (position < 1) {
                showDate = true;
            } else {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                    showDate = sdf.parse(mCurrent.substring(1, 9)).getTime() > sdf.parse(data.get(position - 1).substring(1, 9)).getTime();
                } catch (ParseException e) {
                    showDate = false;
                    e.printStackTrace();
                }
            }
            holder.mDate.setVisibility(showDate ? View.VISIBLE : View.GONE);
            holder.mDate.setText(showDate ? mCurrent.substring(1, 9).replace('/', '.') : "");
            holder.mTime.setText(mCurrent.substring(11, 19));
            holder.mAppName.setText(mCurrent.substring(21));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        protected static class LogViewHolder extends RecyclerView.ViewHolder {

            protected TextView mDate, mTime, mAppName;

            public LogViewHolder(View itemView) {
                super(itemView);
                mDate = itemView.findViewById(R.id.log_item_date);
                mTime = itemView.findViewById(R.id.log_item_time);
                mAppName = itemView.findViewById(R.id.log_item_app_name);
            }
        }
    }
}
