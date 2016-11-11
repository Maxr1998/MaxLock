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

package de.Maxr1998.xposed.maxlock.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.actions.ActionActivity;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static de.Maxr1998.xposed.maxlock.util.MLPreferences.getPreferences;
import static de.Maxr1998.xposed.maxlock.util.MLPreferences.getPreferencesKeys;
import static de.Maxr1998.xposed.maxlock.util.MLPreferences.getPreferencesKeysPerApp;

public final class Util {

    public static final int PATTERN_CODE = 48;
    public static final int PATTERN_CODE_APP = 5;
    public static final String LOG_TAG = "MaxLock";
    public static final String LOG_TAG_STARTUP = "ML-Startup";
    public static final String LOG_TAG_LOCKSCREEN = "ML-Lockscreen";
    public static final String LOG_TAG_TASKER = "ML-Tasker";
    public static final String LOG_TAG_ADMIN = "ML-DeviceAdmin";
    public static final String LOG_TAG_IAB = "ML-IAB";
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static SoftReference<Drawable> WALLPAPER = new SoftReference<>(null);

    private Util() {}

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
        if (!getPreferences(a).getBoolean(Common.USE_DARK_STYLE, false)) {
            a.setTheme(R.style.AppTheme);
        } else {
            if (!getPreferences(a).getBoolean(Common.USE_AMOLED_BLACK, false)) {
                a.setTheme(R.style.AppTheme_Dark);
            } else {
                a.setTheme(R.style.AppTheme_Dark_AMOLED);
            }
        }
    }

    /**
     * Hide keyboard from window
     *
     * @param a Activity to call from
     * @param v View where keyboard is attached
     * @return true if keyboard got hidden, false if the keyboard wasn't visible before
     */
    public static boolean hideKeyboardFromWindow(Activity a, View v) {
        ResultReceiver result = new ResultReceiver(null) {
            private int i = -1;

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                i = resultCode;
            }

            @Override
            public int describeContents() {
                return i;
            }
        };
        a.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        //noinspection ConstantConditions
        ((InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(v.getWindowToken(), 0, result);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result.describeContents() == InputMethodManager.RESULT_HIDDEN;
    }

    public static void getBackground(AppCompatActivity activity, final ImageView background) {
        switch (getPreferences(background.getContext()).getString(Common.BACKGROUND, "")) {
            case "color":
                background.setImageDrawable(new ColorDrawable(getPreferences(background.getContext()).getInt(Common.BACKGROUND_COLOR, ContextCompat.getColor(background.getContext(), R.color.accent))));
                break;
            case "custom":
                try {
                    background.setImageBitmap(BitmapFactory.decodeStream(background.getContext().openFileInput("background")));
                } catch (IOException | OutOfMemoryError e) {
                    background.setImageDrawable(new ColorDrawable(ContextCompat.getColor(background.getContext(), R.color.accent)));
                    Toast.makeText(background.getContext(), "Error loading background image, " + (e instanceof IOException ? ", IOException." : "is it to big?"), Toast.LENGTH_LONG).show();
                }
                break;
            default:
                activity.getSupportLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Drawable>() {
                    @Override
                    public Loader<Drawable> onCreateLoader(int id, Bundle args) {
                        return new AsyncTaskLoader<Drawable>(background.getContext()) {
                            @Override
                            protected void onStartLoading() {
                                super.onStartLoading();
                                forceLoad();
                            }

                            @Override
                            public Drawable loadInBackground() {
                                if (WALLPAPER.get() != null)
                                    return WALLPAPER.get();
                                else
                                    return WallpaperManager.getInstance(getContext()).getFastDrawable();
                            }
                        };
                    }

                    @Override
                    public void onLoadFinished(Loader<Drawable> loader, Drawable data) {
                        if (data != null) {
                            background.setImageDrawable(data);
                            if (WALLPAPER.get() == null)
                                WALLPAPER = new SoftReference<>(data);
                        } else {
                            background.setImageDrawable(new ColorDrawable(ContextCompat.getColor(background.getContext(), R.color.accent)));
                            Toast.makeText(background.getContext(), "Failed to load system wallpaper!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoaderReset(Loader<Drawable> loader) {
                    }
                });
                break;
        }
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
        StringBuilder patternKey = new StringBuilder();
        for (char x : pattern) {
            patternKey.append(x);
        }
        if (app == null) {
            getPreferences(context).edit().putString(Common.LOCKING_TYPE, Common.PREF_VALUE_PATTERN).apply();
            getPreferencesKeys(context).edit().putString(Common.KEY_PREFERENCE, Util.shaHash(patternKey.toString())).apply();
        } else {
            getPreferencesKeysPerApp(context).edit().putString(app, Common.PREF_VALUE_PATTERN).putString(app + Common.APP_KEY_PREFERENCE, Util.shaHash(patternKey.toString())).apply();
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
                        getPreferencesKeys(context).edit().putString(Common.KEY_PREFERENCE, shaHash(v1)).apply();
                        getPreferences(context).edit().putString(Common.LOCKING_TYPE, v1.matches("[0-9]+") ? Common.PREF_VALUE_PASS_PIN : Common.PREF_VALUE_PASSWORD).apply();
                    } else {
                        getPreferencesKeysPerApp(context).edit().putString(app, v1.matches("[0-9]+") ? Common.PREF_VALUE_PASS_PIN : Common.PREF_VALUE_PASSWORD).putString(app + Common.APP_KEY_PREFERENCE, shaHash(v1)).apply();
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

    public static void checkForStoragePermission(final Fragment fragment, final int code, @StringRes int description) {
        if (ContextCompat.checkSelfPermission(fragment.getActivity(), WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(fragment.getActivity())
                    .setMessage(description)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fragment.requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, code);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        } else {
            fragment.onRequestPermissionsResult(code, new String[]{WRITE_EXTERNAL_STORAGE}, new int[]{PackageManager.PERMISSION_GRANTED});
        }
    }

    public static String getApplicationNameFromPackage(String packageName, Context context) {
        if (context instanceof ActionActivity) {
            return packageName;
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            CharSequence label = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0));
            if (label != null) {
                return label.toString();
            } else {
                return packageName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return "(Not found)";
        }
    }

    public static Drawable getApplicationIconFromPackage(String packageName, Context context) {
        try {
            return context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
        }
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

    /**
     * Compresses files from a directory into a zip file.
     *
     * @param directory the directory to compress
     * @param stream    the ZipOutputStream to write the files to
     * @throws IOException
     */
    public static void writeDirectoryToZip(File directory, ZipOutputStream stream) throws IOException {
        writeDirectoryToZip(directory, stream, directory);
    }

    private static void writeDirectoryToZip(File d, ZipOutputStream s, File t) throws IOException {
        for (File f : d.listFiles()) {
            if (f.isDirectory()) {
                writeDirectoryToZip(f, s, t);
                continue;
            }
            String path = f.getAbsolutePath().replace(t.getAbsolutePath(), "");
            ZipEntry entry = new ZipEntry(path);
            s.putNextEntry(entry);
            FileUtils.copyFile(f, s);
            s.closeEntry();
        }
    }
}