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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    final ThemeListFragment mFragment;
    final Context mContext;
    List<Map<String, String>> themes;
    SharedPreferences prefs;

    public ThemeAdapter(ThemeListFragment fragment, Context context, List<Map<String, String>> themes) {
        mFragment = fragment;
        mContext = context;
        this.themes = themes;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.theme_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final String packageName = themes.get(position).get("packageName");

        holder.title.setText(themes.get(position).get("title") + (prefs.getString(Common.THEME_PKG, "").equals(packageName) ? " (APPLIED)" : ""));
        holder.packageName.setText(packageName);
        holder.version.setText(themes.get(position).get("version"));

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFragment.showBottomBar(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return themes.size();
    }

    public Map<String, String> getItem(int position) {
        return themes.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public CardView cardView;
        public TextView title;
        public TextView packageName;
        public TextView version;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            title = (TextView) itemView.findViewById(R.id.title);
            packageName = (TextView) itemView.findViewById(R.id.package_name);
            version = (TextView) itemView.findViewById(R.id.version);
        }
    }
}
