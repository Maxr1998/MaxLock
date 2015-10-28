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

package de.Maxr1998.xposed.maxlock.util;

import android.app.IntentService;
import android.content.Intent;

public class ThemeService extends IntentService {

    /*public static final String THEME_STORE_ASSET_NAME = "theme-store.json";
    public static final String THEME_INSTALL_PATH = "theme/" + THEME_STORE_ASSET_NAME;
    public static final String WALLPAPER_INSTALL_PATH = "theme/wallpaper.png";
    private File mInstalledThemeFile;*/

    public ThemeService() {
        super("ThemeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        /*Log.d("MaxLock/ThemeService", "Intent received");
        mInstalledThemeFile = new File(Util.dataDir(this) + THEME_INSTALL_PATH);
        switch (intent.getIntExtra("extra", -1)) {
            case 1:
                installFrom(intent.getStringExtra("package"));
                break;
            case 2:
                uninstall();
                break;
        }*/
    }

    /*@SuppressLint("WorldReadableFiles")
    private void installFrom(String packageName) {
        AssetManager assets;
        try {
            assets = getPackageManager().getResourcesForApplication(packageName).getAssets();
            // Copy file
            InputStream themeStream = assets.open(THEME_STORE_ASSET_NAME);
            FileUtils.copyInputStreamToFile(themeStream, mInstalledThemeFile);

            // Extract wallpaper
            JSONObject j = new JSONObject(FileUtils.readFileToString(mInstalledThemeFile));
            if (!j.isNull("background_image")) {
                byte[] decodedString = Base64.decode(j.getString("background_image"), Base64.URL_SAFE);
                Bitmap wallpaper = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(Util.dataDir(this) + WALLPAPER_INSTALL_PATH);
                    wallpaper.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            uninstall();
            e.printStackTrace();
        }
    }

    private void uninstall() {
        try {
            FileUtils.deleteDirectory(new File(Util.dataDir(this) + "theme"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}