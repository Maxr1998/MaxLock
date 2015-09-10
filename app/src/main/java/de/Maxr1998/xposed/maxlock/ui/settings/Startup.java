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

package de.Maxr1998.xposed.maxlock.ui.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;

import com.google.android.gms.analytics.HitBuilders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ThisApplication;
import de.Maxr1998.xposed.maxlock.util.Util;

public class Startup extends AsyncTask<Boolean, Void, Void> {

    private final Context mContext;
    private final SharedPreferences prefs;
    // Outpur vars
    private boolean showSnackBar;
    private boolean snackBarMultiLine;
    private String snackBarContent;
    private boolean showDialog;
    private AlertDialog.Builder builder;
    private boolean isFirstStart;

    public Startup(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    protected Void doInBackground(Boolean... firstStart) {
        isFirstStart = firstStart[0];
        if (isFirstStart) {
            prefs.edit().putLong(Common.FIRST_START_TIME, System.currentTimeMillis()).apply();
        }
        // Pro setup
        if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
            prefs.edit().putBoolean(Common.ENABLE_LOGGING, false).apply();
            prefs.edit().putBoolean(Common.ENABLE_IMOD_DELAY_APP, false).apply();
            prefs.edit().putBoolean(Common.ENABLE_IMOD_DELAY_GLOBAL, false).apply();
        }
        // Like app dialog
        if (!prefs.getBoolean(Common.DIALOG_SHOW_NEVER, false) && System.currentTimeMillis() - prefs.getLong(Common.FIRST_START_TIME, System.currentTimeMillis()) > 10 * 24 * 3600 * 1000) {
            showDialog = true;
            builder = new AlertDialog.Builder(mContext);
            @SuppressLint("InflateParams") View dialogView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.dialog_like_app, null);
            @SuppressWarnings("ResourceType") final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.dialog_cb_never_again);
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (checkBox.isChecked())
                        prefs.edit().putBoolean(Common.DIALOG_SHOW_NEVER, true).apply();
                    switch (i) {
                        case -3:
                            try {
                                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Common.PKG_NAME)));
                            } catch (android.content.ActivityNotFoundException e) {
                                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + Common.PKG_NAME)));
                            }
                            break;
                        case -1:

                            break;
                    }
                    prefs.edit().putLong(Common.FIRST_START_TIME, System.currentTimeMillis()).apply();
                }
            };
            builder.setTitle(R.string.dialog_like_app)
                    .setView(dialogView)
                    .setPositiveButton(R.string.dialog_button_donate, onClickListener)
                    .setNeutralButton(R.string.dialog_button_rate, onClickListener)
                    .setNegativeButton(android.R.string.cancel, onClickListener);
        }
        // Other
        Util.cleanUp(mContext);

        List<String> list = new ArrayList<>();
        try {
            @SuppressLint("SdCardPath") BufferedReader br = new BufferedReader(new FileReader("/data/data/de.robv.android.xposed.installer/log/error.log"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("MLaC|") || line.contains("MLiU|")) {
                    list.add(line);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < list.size() - 1; i++) {
            String[] a = list.get(i).split("\\|");
            String[] b = list.get(i + 1).split("\\|");
            if (a[0].endsWith("MLaC") && b[0].endsWith("MLiU") && a[1].equals(b[1]) && Long.parseLong(b[3]) - Long.parseLong(a[3]) < 400) {
                ThisApplication.getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("Launch Activities")
                        .setAction("Unlock")
                        .setLabel(a[1])
                        .setValue(1)
                        .build());
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (isFirstStart) {
            prefs.edit().putBoolean(Common.FIRST_START, false).apply();
        }
        if (showDialog) {
            builder.create().show();
        }
        System.out.println("Startup finished");
    }
}
