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

package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.Util;

public class ThemeService extends IntentService {

    private static final String themeOrigFile = "theme.xml";
    private static SharedPreferences PREFS_THEME;
    public final String backgroundOrigFile = "background.png";
    File themeFile;
    SharedPreferences prefs;


    public ThemeService() {
        super("ThemeService");
    }

    @SuppressLint("WorldReadableFiles")
    public static void loadPrefs(Context context) {
        if (PREFS_THEME == null)
            //noinspection deprecation
            PREFS_THEME = context.getSharedPreferences(Common.PREFS_THEME, MODE_WORLD_READABLE);
    }

    public static ViewGroup.LayoutParams container(final View container, Context context, String lockingType) {
        loadPrefs(context);
        String containerMargin = "container_margin_";
        int density = (int) context.getResources().getDisplayMetrics().density;
        int left = PREFS_THEME.getInt(containerMargin + "left" + "_" + lockingType, 0) * density;
        int right = PREFS_THEME.getInt(containerMargin + "right" + "_" + lockingType, 0) * density;
        int top = PREFS_THEME.getInt(containerMargin + "top" + "_" + lockingType, 0) * density;
        int bottom = PREFS_THEME.getInt(containerMargin + "bottom" + "_" + lockingType, 0) * density;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(container.getLayoutParams());
        layoutParams.setMargins(left, top, right, bottom);
        layoutParams.weight = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            layoutParams.setMarginStart(left);
            layoutParams.setMarginEnd(right);
        }
        return layoutParams;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("MaxLock/ThemeService", "Intent received");
        themeFile = new File(Util.dataDir(this) + "shared_prefs" + File.separator + themeOrigFile);

        int extra = intent.getIntExtra("extra", -1);
        switch (extra) {
            case 1:
                importTheme(intent.getStringExtra("package"));
                break;
            case 2:
                clearUp();
                break;
        }
    }

    @SuppressLint("WorldReadableFiles")
    public void importTheme(String packageName) {
        /**
         * Preferences
         */
        prefs = getSharedPreferences(Common.PREFS, MODE_PRIVATE);
        /**
         * Files
         */
        AssetManager assets;
        try {
            assets = getPackageManager().getResourcesForApplication(packageName).getAssets();

            // theme.xml file
            InputStream themeStream = assets.open(themeOrigFile);
            FileUtils.copyInputStreamToFile(themeStream, themeFile);
            if (themeFile.length() < 10) {
                Toast.makeText(this, "No theme.xml found, exiting...", Toast.LENGTH_SHORT).show();
                //noinspection ResultOfMethodCallIgnored
                themeFile.delete();
                return;
            }
            getSharedPreferences(Common.PREFS_THEME, MODE_MULTI_PROCESS);
            //noinspection deprecation
            getSharedPreferences(Common.PREFS_THEME, MODE_WORLD_READABLE);

            // background.png
            InputStream backgroundStream = assets.open(backgroundOrigFile);
            File backgroundFile = new File(Util.dataDir(this) + "theme" + File.separator + backgroundOrigFile);
            FileUtils.copyInputStreamToFile(backgroundStream, backgroundFile);
            if (themeFile.length() < 10)
                //noinspection ResultOfMethodCallIgnored
                backgroundFile.delete();
            else {
                prefs.edit().putString(Common.BACKGROUND, "theme").apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
            clearUp();
        }
    }

    public void clearUp() {
        /**
         * Preferences
         */
        prefs = getSharedPreferences(Common.PREFS, MODE_PRIVATE);
        prefs.edit()
                .putString(Common.BACKGROUND, "wallpaper")
                .apply();

        /**
         * Files
         */
        //noinspection ResultOfMethodCallIgnored
        themeFile.delete();
        getSharedPreferences("theme", MODE_MULTI_PROCESS);
        File theme = new File(Util.dataDir(this) + "theme");
        try {
            FileUtils.deleteDirectory(theme);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}