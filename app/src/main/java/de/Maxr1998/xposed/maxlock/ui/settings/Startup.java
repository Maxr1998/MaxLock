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

package de.Maxr1998.xposed.maxlock.ui.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.util.Util;

public class Startup extends AsyncTask<Void, Void, Void> {

    private final Context mContext;
    private final SharedPreferences prefs;

    public Startup(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    @Override
    protected Void doInBackground(Void... v) {
        prefs.edit().putInt(Common.RATING_DIALOG_APP_OPENING_COUNTER, prefs.getInt(Common.RATING_DIALOG_APP_OPENING_COUNTER, 0) + 1).apply();

        // Create ML Files
        File historyF = new File(mContext.getFilesDir(), "history.json");
        try {
            if (historyF.getParentFile().mkdirs() || historyF.createNewFile()) {
                Log.i(Util.LOG_TAG_STARTUP, "History created.");
            }
            historyF.setReadable(true, false);
            historyF.setWritable(true, false);
            historyF.setExecutable(true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Non-pro setup
        if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
            prefs.edit().putBoolean(Common.ENABLE_LOGGING, false).apply();
            prefs.edit().putBoolean(Common.ENABLE_IMOD_DELAY_APP, false).apply();
            prefs.edit().putBoolean(Common.ENABLE_IMOD_DELAY_GLOBAL, false).apply();
        }

        // Clean up
        File backgroundFolder = new File(Util.dataDir(mContext), "background");
        if (backgroundFolder.exists()) {
            try {
                FileUtils.copyFile(new File(backgroundFolder, "image"), mContext.openFileOutput("background", 0));
                FileUtils.deleteQuietly(backgroundFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List<File> filesToDelete = new ArrayList<>();
        File[] listPrefs = new File(Util.dataDir(mContext), "shared_prefs").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return !Arrays.asList("com.google.android.gms.analytics.prefs.xml",
                        "de.Maxr1998.xposed.maxlock_preferences.xml",
                        "keys.xml", "packages.xml", "per_app_settings.xml",
                        "WebViewChromiumPrefs.xml").contains(filename);
            }
        });
        if (listPrefs != null) {
            filesToDelete.addAll(Arrays.asList(listPrefs));
        }
        File[] listFiles = mContext.getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return !Arrays.asList("background", "gaClientId", "gaClientIdData", "history.json").contains(filename);
            }
        });
        if (listFiles != null) {
            filesToDelete.addAll(Arrays.asList(listFiles));
        }
        File[] listExternal = new File(Common.EXTERNAL_FILES_DIR).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return !Arrays.asList("Backup", "dev_mode.key").contains(filename);
            }
        });
        if (listExternal != null) {
            filesToDelete.addAll(Arrays.asList(listExternal));
        }
        for (File f : filesToDelete) {
            FileUtils.deleteQuietly(f);
        }
        FileUtils.deleteQuietly(new File(Environment.getExternalStorageDirectory() + "/MaxLock_Backup/"));
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        prefs.edit().putBoolean(Common.FIRST_START, false).apply();
        Log.i(Util.LOG_TAG, "Startup finished");
    }
}
