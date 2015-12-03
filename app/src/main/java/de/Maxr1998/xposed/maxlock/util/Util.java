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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;

public abstract class Util {

    public static final int PATTERN_CODE = 48;
    public static final int PATTERN_CODE_APP = 5;
    public static final String LOG_TAG = "MaxLock";
    public static final String LOG_TAG_STARTUP = "ML-Startup";
    public static final String LOG_TAG_LOCKSCREEN = "ML-Lockscreen";
    public static final String LOG_TAG_ADMIN = "ML-DeviceAdmin";
    public static final String LOG_TAG_IAB = "ML-IAB";
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static Bitmap M_BITMAP;
    private static SharedPreferences PREFS, PREFS_KEY, PREFS_PER_APP;
    private static ApplicationInfo REQUEST_PKG_INFO;
    private static PackageManager PM;

    // Prefs
    private static void loadPrefs(Context context) {
        PREFS = PreferenceManager.getDefaultSharedPreferences(context);
        PREFS_KEY = context.getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        PREFS_PER_APP = context.getSharedPreferences(Common.PREFS_KEYS_PER_APP, Context.MODE_PRIVATE);
    }

    // UI

    /**
     * This method calculates a size in pixels from a given dp value.
     *
     * @param o  Object of either View or Context
     * @param dp Value to convert to pixels
     * @return Calculated Pixels
     */
    public static int dpToPx(@NonNull Object o, int dp) {
        Context c;
        if (o instanceof View) {
            c = ((View) o).getContext();
        } else if (o instanceof Context) {
            c = (Context) o;
        } else {
            throw new IllegalArgumentException("This object only takes views or contexts as argument!");
        }
        return (int) (c.getResources().getDisplayMetrics().density * dp);
    }

    public static void setTheme(Activity a) {
        loadPrefs(a);
        if (!PREFS.getBoolean(Common.USE_DARK_STYLE, false)) {
            a.setTheme(R.style.AppTheme);
        } else {
            if (!PREFS.getBoolean(Common.USE_AMOLED_BLACK, false)) {
                a.setTheme(R.style.AppTheme_Dark);
            } else {
                a.setTheme(R.style.AppTheme_Dark_AMOLED);
            }
        }
    }

    public static void hideKeyboardFromWindow(Activity a, View v) {
        a.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        //noinspection ConstantConditions
        ((InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static Drawable getBackground(Context context, int viewWidth, int viewHeight) {
        loadPrefs(context);
        String backgroundType = PREFS.getString(Common.BACKGROUND, "wallpaper");
        switch (backgroundType) {
            case "theme":
                InputStream backgroundStream;
                try {
                    File backgroundFile = new File(dataDir(context) + "theme/background.png");
                    if (backgroundFile.exists()) {
                        backgroundStream = new FileInputStream(backgroundFile);
                        M_BITMAP = BitmapFactory.decodeStream(backgroundStream);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "custom":
                InputStream inputStream;
                try {
                    inputStream = new FileInputStream(new File(dataDir(context) + "background/image"));
                    M_BITMAP = BitmapFactory.decodeStream(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (OutOfMemoryError e) {
                    M_BITMAP = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
                    M_BITMAP.eraseColor(PREFS.getInt(Common.BACKGROUND_COLOR, 0));
                    Toast.makeText(context, "Error loading background, is it to big?", Toast.LENGTH_LONG).show();
                }
                break;
            case "color":
                M_BITMAP = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
                M_BITMAP.eraseColor(PREFS.getInt(Common.BACKGROUND_COLOR, 0));
                break;
            default:
                Drawable wallpaper = WallpaperManager.getInstance(context).getDrawable();
                if (wallpaper == null) {
                    M_BITMAP = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
                    M_BITMAP.eraseColor(ContextCompat.getColor(context, R.color.accent));
                } else {
                    M_BITMAP = ((BitmapDrawable) wallpaper).getBitmap();
                }
                break;
        }
        return new BitmapDrawable(context.getResources(), M_BITMAP);
    }

    // Lock
    public static String shaHash(String toHash) { // from: [ http://stackoverflow.com/a/11978976 ]. Thanks very much!
        String hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = toHash.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();

            hash = bytesToHex(bytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return hash;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void receiveAndSetPattern(Context context, char[] pattern, String app) {
        loadPrefs(context);
        StringBuilder patternKey = new StringBuilder();
        for (char x : pattern) {
            patternKey.append(x);
        }
        if (app == null) {
            PREFS.edit().putString(Common.LOCKING_TYPE, Common.PREF_VALUE_PATTERN).apply();
            PREFS_KEY.edit().putString(Common.KEY_PREFERENCE, Util.shaHash(patternKey.toString())).apply();
        } else {
            PREFS_PER_APP.edit().putString(app, Common.PREF_VALUE_PATTERN).putString(app + Common.APP_KEY_PREFERENCE, Util.shaHash(patternKey.toString())).apply();
        }
    }

    public static int getPatternCode(int app) {
        if (app == -1) {
            return PATTERN_CODE;
        } else {
            int code = Integer.valueOf(String.valueOf(PATTERN_CODE_APP) + String.valueOf(app));
            System.out.println(code);
            return code;
        }
    }

    public static void setPassword(final Context context, final String app) {
        loadPrefs(context);
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_password, null);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.pref_set_password)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        ((ViewGroup) dialogView.getParent()).setPadding(10, 10, 10, 10);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText p1 = (EditText) dialogView.findViewById(R.id.edt_password);
                EditText p2 = (EditText) dialogView.findViewById(R.id.edt_re_password);
                String v1 = p1.getText().toString();
                String v2 = p2.getText().toString();

                if (!v1.equals(v2)) {
                    p1.setText("");
                    p2.setText("");
                    Toast.makeText(context, R.string.toast_password_inconsistent, Toast.LENGTH_SHORT)
                            .show();
                } else if (v1.length() == 0) {
                    Toast.makeText(context, R.string.toast_password_null, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    dialog.dismiss();
                    if (app == null) {
                        PREFS_KEY.edit().putString(Common.KEY_PREFERENCE, shaHash(v1)).apply();
                        PREFS.edit().putString(Common.LOCKING_TYPE, v1.matches("[0-9]+") ? Common.PREF_VALUE_PASS_PIN : Common.PREF_VALUE_PASSWORD).apply();
                    } else {
                        PREFS_PER_APP.edit().putString(app, v1.matches("[0-9]+") ? Common.PREF_VALUE_PASS_PIN : Common.PREF_VALUE_PASSWORD).putString(app + Common.APP_KEY_PREFERENCE, shaHash(v1)).apply();
                    }
                    Toast.makeText(context, R.string.toast_password_changed, Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public static void logFailedAuthentication(Context context, String pkg) {
        String toLog = "[" + new SimpleDateFormat("dd/MM/yy, HH:mm:ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())) + "] " + getApplicationNameFromPackage(pkg, context);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(context.getApplicationInfo().dataDir + File.separator + Common.LOG_FILE, true)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (writer != null) {
            writer.printf("%s" + "%n", toLog);
            writer.close();
        }
    }

    // Packages
    public static String dataDir(Context context) {
        return context.getApplicationInfo().dataDir + File.separator;
    }

    public static String getApplicationNameFromPackage(String packageName, Context context) {
        PM = context.getApplicationContext().getPackageManager();
        try {
            REQUEST_PKG_INFO = PM.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            REQUEST_PKG_INFO = null;
        }
        return (String) (REQUEST_PKG_INFO != null ? PM.getApplicationLabel(REQUEST_PKG_INFO) : packageName);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static Drawable getApplicationIconFromPackage(String packageName, Context context) {
        PM = context.getApplicationContext().getPackageManager();
        try {
            REQUEST_PKG_INFO = PM.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            REQUEST_PKG_INFO = null;
        }
        return REQUEST_PKG_INFO != null ? PM.getApplicationIcon(REQUEST_PKG_INFO) :
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                        context.getResources().getDrawable(R.mipmap.ic_launcher) :
                        context.getDrawable(R.mipmap.ic_launcher));
    }

    public static String getLanguageCode() {
        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();
        if (language.equals("")) {
            return "en-GB";
        }
        if (!country.equals("")) {
            return language + "-" + country;
        } else return language;
    }

    public static boolean isDevMode() {
        try {
            BufferedReader r = new BufferedReader(new FileReader(Common.EXTERNAL_FILES_DIR + "dev_mode.key"));
            return Util.shaHash(r.readLine()).toLowerCase().equals("08b49da56ef8f5bf0aa51c64d5e683ba3e7599bd6e2e3906e584fca14cb95f82");
        } catch (Exception e) {
            return false;
        }
    }
}