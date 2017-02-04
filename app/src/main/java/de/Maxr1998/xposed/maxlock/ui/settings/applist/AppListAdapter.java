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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.haibison.android.lockpattern.LockPatternActivity;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.MaxLockPreferenceFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.KnockCodeSetupFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.lockingtype.PinSetupFragment;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

@SuppressLint("CommitPrefEdits")
class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppsListViewHolder> implements Filterable, FastScrollRecyclerView.SectionedAdapter {

    private final Fragment mFragment;
    private final Context mContext;
    private final SharedPreferences prefsApps, prefsKeysPerApp;
    private final AppFilter mFilter;

    AppListAdapter(Fragment fragment) {
        mFragment = fragment;
        mContext = fragment.getActivity();
        prefsApps = MLPreferences.getPrefsApps(mContext);
        prefsKeysPerApp = mContext.getSharedPreferences(Common.PREFS_KEYS_PER_APP, Context.MODE_PRIVATE);
        mFilter = new AppFilter();
    }

    @Override
    public AppsListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
        return new AppsListViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final AppsListViewHolder hld, int position) {
        final String key = ListHolder.getInstance().get(position).packageName;
        hld.tag = key;
        hld.prefsApps = prefsApps;

        hld.appIcon.setImageDrawable(ListHolder.getInstance().get(position).loadIcon(mContext.getPackageManager()));
        hld.appIcon.setContentDescription(mContext.getString(R.string.content_description_applist_icon, nameAt(position)));
        hld.appName.setText(nameAt(position));
        boolean locked = prefsApps.getBoolean(key, false);
        hld.options.setVisibility(locked ? View.VISIBLE : View.GONE);
        hld.options.setContentDescription(mContext.getString(R.string.content_description_applist_options, hld.appName.getText()));
        hld.toggle.setChecked(locked);
        hld.toggle.setContentDescription(mContext.getString(locked ? R.string.content_description_applist_toggle_on : R.string.content_description_applist_toggle_off, hld.appName.getText()));

        hld.options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder optionsDialog = new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.dialog_title_options))
                        .setIcon(ListHolder.getInstance().get(hld.getAdapterPosition()).loadIcon(mContext.getPackageManager()))
                        .setMultiChoiceItems(R.array.dialog_multi_select_items_options,
                                new boolean[]{
                                        prefsKeysPerApp.contains(key),
                                        prefsApps.getBoolean(key + "_fake", false),
                                        prefsApps.getBoolean(key + "_notif_content", false),
                                },
                                new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                        String curKey = null;
                                        switch (which) {
                                            case 0:
                                                if (isChecked) {
                                                    dialog.dismiss();
                                                    new AlertDialog.Builder(mContext)
                                                            .setItems(new CharSequence[]{
                                                                            mContext.getString(R.string.pref_locking_type_password),
                                                                            mContext.getString(R.string.pref_locking_type_pin),
                                                                            mContext.getString(R.string.pref_locking_type_knockcode),
                                                                            mContext.getString(R.string.pref_locking_type_pattern)
                                                                    },
                                                                    new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int i) {
                                                                            dialog.dismiss();
                                                                            Fragment frag = new Fragment();
                                                                            switch (i) {
                                                                                case 0:
                                                                                    Util.setPassword(mContext, key);
                                                                                    return;
                                                                                case 1:
                                                                                    frag = new PinSetupFragment();
                                                                                    break;
                                                                                case 2:
                                                                                    frag = new KnockCodeSetupFragment();
                                                                                    break;
                                                                                case 3:
                                                                                    Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, mContext, LockPatternActivity.class);
                                                                                    mFragment.startActivityForResult(intent, Util.getPatternCode(hld.getAdapterPosition()));
                                                                                    return;
                                                                            }
                                                                            Bundle b = new Bundle(1);
                                                                            b.putString(Common.INTENT_EXTRAS_CUSTOM_APP, key);
                                                                            frag.setArguments(b);
                                                                            MaxLockPreferenceFragment.launchFragment(mFragment.getFragmentManager(), frag, false);
                                                                        }
                                                                    }).show();
                                                } else {
                                                    prefsKeysPerApp.edit().remove(key).remove(key + Common.APP_KEY_PREFERENCE).apply();
                                                }
                                                return;
                                            case 1:
                                                curKey = key + "_fake";
                                                break;
                                            case 2:
                                                curKey = key + "_notif_content";
                                                break;
                                        }
                                        if (curKey != null) {
                                            if (isChecked)
                                                prefsApps.edit().putBoolean(curKey, true).commit();
                                            else
                                                prefsApps.edit().remove(curKey).commit();
                                        }
                                    }
                                })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(R.string.dialog_button_exclude_activities, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new ActivityLoader().execute(key);
                            }
                        });
                optionsDialog.create().show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return ListHolder.getInstance().size();
    }

    private String nameAt(int position) {
        return ListHolder.getInstance().get(position < getItemCount() ? position : getItemCount() - 1).loadLabel(mContext.getPackageManager()).toString();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        return (nameAt(position) + "?").substring(0, 1).toUpperCase();
    }

    @Override
    public AppFilter getFilter() {
        return mFilter;
    }

    static class AppsListViewHolder extends RecyclerView.ViewHolder {

        final ImageView appIcon;
        final TextView appName;
        final ImageButton options;
        final ToggleButton toggle;
        String tag;
        SharedPreferences prefsApps;

        AppsListViewHolder(View itemView) {
            super(itemView);
            appIcon = (ImageView) itemView.findViewById(R.id.icon);
            appName = (TextView) itemView.findViewById(R.id.title);
            options = (ImageButton) itemView.findViewById(R.id.edit);
            toggle = (ToggleButton) itemView.findViewById(R.id.toggleLock);

            // Launch app when tapping icon
            appIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent launch = v.getContext().getPackageManager().getLaunchIntentForPackage(tag);
                    if (launch == null) {
                        return;
                    }
                    launch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    v.getContext().startActivity(launch);
                }
            });

            // Turn lock on/off
            toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    boolean value = ((ToggleButton) v).isChecked();
                    if (value) {
                        prefsApps.edit().putBoolean(tag, true).commit();
                        AnimationSet in = (AnimationSet) AnimationUtils.loadAnimation(v.getContext(), R.anim.applist_settings);
                        options.startAnimation(in);
                        options.setVisibility(View.VISIBLE);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && tag.equals("com.android.settings")) {
                            new AlertDialog.Builder(v.getContext())
                                    .setMessage(R.string.known_problem_settings_message)
                                    .setNeutralButton(R.string.more_information, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int i) {
                                            @SuppressWarnings("deprecation") CustomTabsIntent knownProblemSettings = new CustomTabsIntent.Builder(((SettingsActivity) v.getContext()).getSession())
                                                    .setShowTitle(true)
                                                    .enableUrlBarHiding()
                                                    .setToolbarColor(v.getResources().getColor(R.color.primary_red))
                                                    .build();
                                            knownProblemSettings.launchUrl(v.getContext(), Common.KNOWN_PROBLEM_SETTINGS_URI);
                                        }
                                    })
                                    .setPositiveButton(android.R.string.ok, null)
                                    .create().show();
                        }
                    } else {
                        prefsApps.edit().remove(tag).commit();
                        AnimationSet out = (AnimationSet) AnimationUtils.loadAnimation(v.getContext(), R.anim.applist_settings_out);
                        out.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                animation = new TranslateAnimation(0.0f, 0.0f, 0.0f, 0.0f);
                                animation.setDuration(1);
                                options.startAnimation(animation);
                                options.setVisibility(View.GONE);
                                options.clearAnimation();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        options.startAnimation(out);
                    }
                    toggle.setContentDescription(v.getContext().getString(value ? R.string.content_description_applist_toggle_on : R.string.content_description_applist_toggle_off, appName.getText()));
                }
            });
        }
    }

    private static class ActivityListAdapter extends RecyclerView.Adapter<ActivityListViewHolder> {

        private final List<String> activities;
        private final Context mContext;
        private final SharedPreferences prefsApps;

        ActivityListAdapter(Context context, List<String> list) {
            mContext = context;
            activities = list;
            prefsApps = MLPreferences.getPrefsApps(mContext);
        }

        @Override
        public ActivityListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_activities_item, parent, false);
            return new ActivityListViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ActivityListViewHolder lvh, int position) {
            String name = activities.get(lvh.getLayoutPosition());
            lvh.switchCompat.setChecked(prefsApps.getBoolean(name, true));
            lvh.switchCompat.setText(name);
            lvh.switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    String now = activities.get(lvh.getLayoutPosition());
                    System.out.println(now + " new Value: " + b);
                    if (b) {
                        prefsApps.edit().remove(now).commit();
                    } else {
                        prefsApps.edit().putBoolean(now, false).commit();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return activities.size();
        }
    }

    private static class ActivityListViewHolder extends RecyclerView.ViewHolder {

        final SwitchCompat switchCompat;

        ActivityListViewHolder(View itemView) {
            super(itemView);
            switchCompat = (SwitchCompat) itemView.findViewById(R.id.activity_switch);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                switchCompat.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    class AppFilter extends Filter {

        @SuppressLint("DefaultLocale")
        @Override
        protected FilterResults performFiltering(CharSequence filter) {
            List<ApplicationInfo> backup = ListHolder.getInstance().backup();
            final String search = filter.toString().toLowerCase();
            final String defaultFilter = MLPreferences.getPreferences(mContext).getString("app_list_filter", "");
            if (search.length() == 0 && defaultFilter.length() == 0) {
                return null;
            } else {
                FilterResults results = new FilterResults();
                List<ApplicationInfo> filteredList = new ArrayList<>();
                for (int i = 0; i < backup.size(); i++) {
                    boolean add = false;
                    if (search.length() != 0) {
                        if (search.equals(backup.get(i).packageName.toLowerCase())) {
                            add = true;
                        } else {
                            String title = backup.get(i).loadLabel(mContext.getPackageManager()).toString().toLowerCase();
                            if (title.startsWith(search)) {
                                add = true;
                            }
                            // Spaces/multiple words in title
                            if (!add)
                                for (String titlePart : title.split(" ")) {
                                    if (titlePart.startsWith(search)) {
                                        add = true;
                                        break;
                                    }
                                }
                            // Spaces/multiple words in search
                            if (!add)
                                for (String searchPart : search.split(" ")) {
                                    if (title.startsWith(searchPart)) {
                                        add = true;
                                        break;
                                    }
                                }
                        }
                    } else {
                        add = true;
                    }
                    boolean isEnabled = prefsApps.getBoolean(backup.get(i).packageName, false);
                    if (add && (defaultFilter.equals("") || (isEnabled && defaultFilter.equals("@*activated*")) || (!isEnabled && defaultFilter.equals("@*deactivated*")))) {
                        filteredList.add(backup.get(i));
                    }
                }
                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results != null) {
                ListHolder.getInstance().setItems((List<ApplicationInfo>) results.values);
            } else {
                ListHolder.getInstance().restore();
            }
            notifyDataSetChanged();
        }
    }

    private class ActivityLoader extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... strings) {
            List<String> list = new ArrayList<>();
            try {
                ActivityInfo[] test = mContext.getPackageManager().getPackageInfo(strings[0], PackageManager.GET_ACTIVITIES).activities;
                for (ActivityInfo info : test) {
                    list.add(info.name);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Collections.sort(list);
            return list;
        }

        @Override
        protected void onPostExecute(List<String> list) {
            super.onPostExecute(list);
            RecyclerView recyclerView = new RecyclerView(mContext);
            recyclerView.setAdapter(new ActivityListAdapter(mContext, list));
            recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            new AlertDialog.Builder(mContext).setTitle(R.string.dialog_title_exclude_activities).setView(recyclerView).show();
        }
    }
}