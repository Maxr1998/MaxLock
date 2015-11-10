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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.Util;

public class Startup extends AsyncTask<Boolean, Void, Void> {

    private final Context mContext;
    private final SharedPreferences prefs;
    private boolean showDialog;
    private AlertDialog.Builder builder;
    private boolean isFirstStart;

    public Startup(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    @Override
    protected Void doInBackground(Boolean... firstStart) {
        isFirstStart = firstStart[0];
        if (isFirstStart) {
            prefs.edit().putLong(Common.FIRST_START_TIME, System.currentTimeMillis()).apply();
        }

        // Create ML Files
        File tmpF = new File(mContext.getFilesDir(), "temps.json");
        File historyF = new File(mContext.getFilesDir(), "history.json");
        try {
            if (tmpF.getParentFile().mkdirs() || tmpF.createNewFile()) {
                Log.i(Util.LOG_TAG_STARTUP, "Temp-file created.");
            }
            if (historyF.getParentFile().mkdirs() || historyF.createNewFile()) {
                Log.i(Util.LOG_TAG_STARTUP, "History created.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        tmpF.setReadable(true, false);
        tmpF.setWritable(true, false);
        tmpF.setExecutable(true, false);
        historyF.setReadable(true, false);
        historyF.setWritable(true, false);
        historyF.setExecutable(true, false);

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
        if (!prefs.getString("migrated", "").equals("v5.3.2")) {
            new File(Util.dataDir(mContext) + "shared_prefs/activities.xml").delete();
            new File(Util.dataDir(mContext) + "shared_prefs/temps.xml").delete();
            new File(Util.dataDir(mContext) + "shared_prefs/imod_temp_values").delete();
            prefs.edit().putString("migrated", "v5.3.2").apply();
        }
        try {
            FileUtils.deleteDirectory(new File(Environment.getExternalStorageDirectory() + "/MaxLock_Backup/"));
            File external = new File(Common.EXTERNAL_FILES_DIR);
            for (File file : external.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return !Arrays.asList("Backup", "dev_mode.key").contains(filename);
                }
            })) {
                FileUtils.forceDelete(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        Log.i(Util.LOG_TAG, "Startup finished");
    }
}
