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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppsListViewHolder> implements SectionIndexer {


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
    public AppsListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_apps_item, parent, false);
        return new AppsListViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final AppsListViewHolder hld, final int position) {
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
                        .setTitle(mContext.getString(R.string.dialog_title_settings))
                        .setIcon(dIcon)
                        .setView(checkBoxView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dlg, int id) {
                                dlg.dismiss();
                            }
                        }).setNeutralButton(R.string.dialog_button_exclude_activities, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new ActivityLoader().execute(key);
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
                    AnimationSet anim = new AnimationSet(true);
                    anim.addAnimation(AnimationUtils.loadAnimation(mContext, R.anim.appslist_settings_rotate));
                    anim.addAnimation(AnimationUtils.loadAnimation(mContext, R.anim.appslist_settings_translate));
                    hld.options.startAnimation(anim);
                    hld.options.setVisibility(View.VISIBLE);
                } else {
                    prefsPackages.edit().remove(key).commit();
                    AnimationSet animOut = new AnimationSet(true);
                    animOut.addAnimation(AnimationUtils.loadAnimation(mContext, R.anim.appslist_settings_rotate_out));
                    animOut.addAnimation(AnimationUtils.loadAnimation(mContext, R.anim.appslist_settings_translate_out));
                    animOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            animation = new TranslateAnimation(0.0f, 0.0f, 0.0f, 0.0f);
                            animation.setDuration(1);
                            hld.options.startAnimation(animation);
                            hld.options.setVisibility(View.GONE);
                            hld.options.clearAnimation();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    hld.options.startAnimation(animOut);
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

    public static class AppsListViewHolder extends RecyclerView.ViewHolder {

        public ImageView appIcon;
        public TextView appName;
        public ImageButton options;
        public ToggleButton toggle;

        public AppsListViewHolder(View itemView) {
            super(itemView);
            appIcon = (ImageView) itemView.findViewById(R.id.icon);
            appName = (TextView) itemView.findViewById(R.id.title);
            options = (ImageButton) itemView.findViewById(R.id.edit);
            toggle = (ToggleButton) itemView.findViewById(R.id.toggleLock);
        }
    }

    private static class ActivityListAdapter extends RecyclerView.Adapter<ActivityListViewHolder> {

        private final List<String> activities;
        private final Context mContext;
        private final SharedPreferences prefsActivities;

        @SuppressWarnings("deprecation")
        @SuppressLint("WorldReadableFiles")
        public ActivityListAdapter(Context context, List<String> list) {
            mContext = context;
            activities = list;
            prefsActivities = mContext.getSharedPreferences(Common.PREFS_ACTIVITIES, Context.MODE_WORLD_READABLE);
        }

        @Override
        public ActivityListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_activities_item, parent, false);
            return new ActivityListViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ActivityListViewHolder lvh, int position) {
            String name = activities.get(lvh.getLayoutPosition());
            lvh.switchCompat.setChecked(prefsActivities.getBoolean(name, true));
            lvh.switchCompat.setText(name);
            lvh.switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    String now = activities.get(lvh.getLayoutPosition());
                    System.out.println(now + " new Value: " + b);
                    if (b) {
                        prefsActivities.edit().remove(now).commit();
                    } else {
                        prefsActivities.edit().putBoolean(now, false).commit();
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

        public SwitchCompat switchCompat;

        public ActivityListViewHolder(View itemView) {
            super(itemView);
            switchCompat = (SwitchCompat) itemView.findViewById(R.id.activity_switch);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                switchCompat.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
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