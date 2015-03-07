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
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.haibison.android.lockpattern.LockPatternActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.KnockCodeSetupFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.PinSetupFragment;

@SuppressLint("CommitPrefEdits")
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> implements SectionIndexer {


    private final List<Map<String, Object>> oriItemList;
    private final Fragment mFragment;
    private final Context mContext;
    private final SharedPreferences prefsPackages, prefsPerApp;
    private final Filter mFilter;
    private List<Map<String, Object>> mItemList;
    private AlertDialog dialog;

    @SuppressLint("WorldReadableFiles")
    public AppListAdapter(Fragment fragment, Context context, List<Map<String, Object>> itemList) {
        mFragment = fragment;
        mContext = context;
        oriItemList = mItemList = itemList;
        //noinspection deprecation
        prefsPackages = mContext.getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE);
        prefsPerApp = mContext.getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_PRIVATE);
        mFilter = new MyFilter();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder hld, final int position) {
        final String sTitle = (String) mItemList.get(position).get("title");
        final String key = (String) mItemList.get(position).get("key");
        final Drawable dIcon = (Drawable) mItemList.get(position).get("icon");

        hld.appName.setText(sTitle);
        hld.appIcon.setImageDrawable(dIcon);

        if (prefsPackages.getBoolean(key, false)) {
            hld.toggle.setChecked(true);
            hld.options.setVisibility(View.VISIBLE);
        } else {
            hld.toggle.setChecked(false);
            hld.options.setVisibility(View.GONE);
        }

        hld.appIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (key.equals("com.android.packageinstaller"))
                    return;
                ActivityManager am = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                am.killBackgroundProcesses(key);
                Intent it = mContext.getPackageManager().getLaunchIntentForPackage(key);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(it);
            }
        });

        hld.options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // AlertDialog View
                // Fake die checkbox
                View checkBoxView = View.inflate(mContext, R.layout.per_app_settings, null);
                CheckBox fakeDie = (CheckBox) checkBoxView.findViewById(R.id.cb_fake_die);
                fakeDie.setChecked(prefsPackages.getBoolean(key + "_fake", false));
                fakeDie.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityManager am = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                        am.killBackgroundProcesses(key);
                        CheckBox cb = (CheckBox) v;
                        boolean value = cb.isChecked();
                        prefsPackages.edit()
                                .putBoolean(key + "_fake", value)
                                .commit();
                    }
                });
                // Custom password checkbox
                CheckBox customPassword = (CheckBox) checkBoxView.findViewById(R.id.cb_custom_pw);
                customPassword.setChecked(prefsPerApp.contains(key));
                customPassword.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        boolean checked = cb.isChecked();
                        if (checked) {
                            dialog.dismiss();
                            final AlertDialog.Builder choose_lock = new AlertDialog.Builder(mContext);
                            CharSequence[] cs = new CharSequence[]{
                                    mContext.getString(R.string.pref_locking_type_password),
                                    mContext.getString(R.string.pref_locking_type_pin),
                                    mContext.getString(R.string.pref_locking_type_knockcode),
                                    mContext.getString(R.string.pref_locking_type_pattern)
                            };
                            choose_lock.setItems(cs, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    Fragment frag = new Fragment();
                                    switch (i) {
                                        case 0:
                                            Util.setPassword(mContext, key);
                                            break;
                                        case 1:
                                            frag = new PinSetupFragment();
                                            break;
                                        case 2:
                                            frag = new KnockCodeSetupFragment();
                                            break;
                                        case 3:
                                            Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, mContext, LockPatternActivity.class);
                                            mFragment.startActivityForResult(intent, Util.getPatternCode(position));
                                            break;
                                    }
                                    if (i == 1 || i == 2) {
                                        Bundle b = new Bundle(1);
                                        b.putString(Common.INTENT_EXTRAS_CUSTOM_APP, key);
                                        frag.setArguments(b);
                                        ((SettingsActivity) mContext).getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).addToBackStack(null).commit();
                                    }
                                }
                            }).show();
                        } else
                            prefsPerApp.edit().remove(key).remove(key + Common.APP_KEY_PREFERENCE).apply();

                    }
                });
                // Finish dialog
                dialog = new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.txt_settings))
                        .setIcon(dIcon)
                        .setView(checkBoxView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dlg, int id) {
                                dlg.dismiss();
                            }
                        }).show();
            }
        });
        hld.toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityManager am = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                am.killBackgroundProcesses(key);

                ToggleButton tb = (ToggleButton) v;

                boolean value = tb.isChecked();
                if (value) {
                    prefsPackages.edit()
                            .putBoolean(key, true)
                            .commit();
                    // TO-DO: Custom reveal animations
                    hld.options.setVisibility(View.VISIBLE);
                } else {
                    prefsPackages.edit().remove(key).commit();
                    hld.options.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    public String nameAt(int position) {
        return (String) mItemList.get(position != getItemCount() ? position : position - 1).get("title");
    }

    public Filter getFilter() {
        return mFilter;
    }

    @Override
    public int getSectionForPosition(int position) {
        return Arrays.asList(getSections()).indexOf(nameAt(position).substring(0, 1).toUpperCase());
    }

    @Override
    public int getPositionForSection(int i) {
        return 0;
    }

    @Override
    public String[] getSections() {
        return "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView appIcon;
        public TextView appName;
        public ImageButton options;
        public ToggleButton toggle;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = (ImageView) itemView.findViewById(R.id.icon);
            appName = (TextView) itemView.findViewById(R.id.title);
            options = (ImageButton) itemView.findViewById(R.id.edit);
            toggle = (ToggleButton) itemView.findViewById(R.id.toggleLock);
        }
    }

    class MyFilter extends Filter {

        @SuppressLint("DefaultLocale")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            constraint = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            if (constraint.length() == 0) {
                results.values = oriItemList;
                results.count = oriItemList.size();
            } else {
                List<Map<String, Object>> filteredList = new ArrayList<>();

                for (Map<String, Object> app : oriItemList) {
                    String title = ((String) app.get("title")).toLowerCase();
                    for (String part : title.split(" ")) {
                        if (part.indexOf((String) constraint) == 0) {
                            filteredList.add(app);
                        }
                    }
                }
                results.values = filteredList;
                results.count = filteredList.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mItemList = (List<Map<String, Object>>) results.values;
            notifyDataSetChanged();
        }
    }
}