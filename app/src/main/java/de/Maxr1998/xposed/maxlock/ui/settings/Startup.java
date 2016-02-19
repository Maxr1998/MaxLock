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
import java.util.Arrays;

import de.Maxr1998.xposed.maxlock.BuildConfig;
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

        // Pro setup
        if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
            prefs.edit().putBoolean(Common.ENABLE_LOGGING, false).apply();
            prefs.edit().putBoolean(Common.ENABLE_IMOD_DELAY_APP, false).apply();
            prefs.edit().putBoolean(Common.ENABLE_IMOD_DELAY_GLOBAL, false).apply();
        }
        // Clean up
        if (prefs.getInt(Common.LAST_VERSION_NUMBER, 0) != BuildConfig.VERSION_CODE) {
            File backgroundFolder = new File(Util.dataDir(mContext), "background");
            if (backgroundFolder.exists()) {
                try {
                    FileUtils.copyFile(new File(backgroundFolder, "image"), mContext.openFileOutput("background", 0));
                    FileUtils.deleteQuietly(backgroundFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            FileUtils.deleteQuietly(new File(mContext.getFilesDir(), "temps.json"));
            FileUtils.deleteQuietly(new File(Util.dataDir(mContext) + "shared_prefs/activities.xml"));
            FileUtils.deleteQuietly(new File(Util.dataDir(mContext) + "shared_prefs/temps.xml"));
            FileUtils.deleteQuietly(new File(Util.dataDir(mContext) + "shared_prefs/imod_temp_values.xml"));
            FileUtils.deleteQuietly(new File(Environment.getExternalStorageDirectory() + "/MaxLock_Backup/"));
            File external = new File(Common.EXTERNAL_FILES_DIR);
            File[] listExternal = external.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return !Arrays.asList("Backup", "dev_mode.key").contains(filename);
                }
            });
            if (listExternal != null) {
                for (File file : listExternal) {
                    FileUtils.deleteQuietly(file);
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        prefs.edit().putBoolean(Common.FIRST_START, false).apply();
        Log.i(Util.LOG_TAG, "Startup finished");
    }
}
