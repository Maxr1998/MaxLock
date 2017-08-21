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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.util.Util;

public class Startup extends AsyncTask<Void, Void, Void> {

    private final Context mContext; // TODO Prevent leaking Context
    private final SharedPreferences prefs;

    public Startup(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    protected Void doInBackground(Void... v) {
        prefs.edit().putInt(Common.RATING_DIALOG_APP_OPENING_COUNTER, prefs.getInt(Common.RATING_DIALOG_APP_OPENING_COUNTER, 0) + 1).apply();

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
        File[] listPrefs = new File(Util.dataDir(mContext), "shared_prefs").listFiles((dir, filename) -> !Arrays.asList("com.google.android.gms.analytics.prefs.xml",
                xml(Common.MAXLOCK_PACKAGE_NAME.concat("_preferences")),
                xml(Common.PREFS_KEY), xml(Common.PREFS_APPS), xml(Common.MAXLOCK_PACKAGE_NAME), xml(Common.PREFS_KEYS_PER_APP),
                xml("WebViewChromiumPrefs")).contains(filename));
        if (listPrefs != null) {
            filesToDelete.addAll(Arrays.asList(listPrefs));
        }
        File[] listFiles = mContext.getFilesDir().listFiles((dir, filename) -> !Arrays.asList("background", "gaClientId", "gaClientIdData", "history.json").contains(filename));
        if (listFiles != null) {
            filesToDelete.addAll(Arrays.asList(listFiles));
        }
        File[] listExternal = new File(Common.EXTERNAL_FILES_DIR).listFiles((dir, filename) -> !Arrays.asList("Backup", "dev_mode.key").contains(filename));
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

    private String xml(String name) {
        return name.concat(".xml");
    }
}