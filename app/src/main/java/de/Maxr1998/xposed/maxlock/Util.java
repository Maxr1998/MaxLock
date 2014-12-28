package de.Maxr1998.xposed.maxlock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

public class Util {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    static AuthenticationSucceededListener authenticationSucceededListener;
    private static int M_COLOR;
    private static Bitmap M_BITMAP;
    private static SharedPreferences PREFS, PREFS_KEY, PREFS_PER_APP;
    private static ApplicationInfo REQUEST_PKG_INFO;
    private static PackageManager PM;

    public static void askPassword(final Context context) {
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(context.getClass().getSimpleName() + "must implement AuthenticationSucceededListener to use this fragment", e);
        }
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(context).inflate(R.layout.ask_password, null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.pref_locking_type_password)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText input = (EditText) dialogView.findViewById(R.id.ent_password);
                String val = input.getText().toString();
                if (Util.checkInput(val, Common.PREF_VALUE_PASSWORD, context, context.getPackageName())) {
                    authenticationSucceededListener.onAuthenticationSucceeded();
                    dialog.dismiss();
                } else {
                    input.setText("");
                    Toast.makeText(context, R.string.msg_password_incorrect, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static void setPassword(final Context context, final String app) {
        PREFS = PreferenceManager.getDefaultSharedPreferences(context);
        PREFS_KEY = context.getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        PREFS_PER_APP = context.getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_PRIVATE);

        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(context).inflate(R.layout.set_password, null);

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

            @SuppressLint("CommitPrefEdits")
            @Override
            public void onClick(View v) {
                EditText p1 = (EditText) dialogView.findViewById(R.id.edt_password);
                EditText p2 = (EditText) dialogView.findViewById(R.id.edt_re_password);
                String v1 = p1.getText().toString();
                String v2 = p2.getText().toString();

                if (!v1.equals(v2)) {
                    p1.setText("");
                    p2.setText("");
                    Toast.makeText(context, R.string.msg_password_inconsistent, Toast.LENGTH_SHORT)
                            .show();
                } else if (v1.length() == 0) {
                    Toast.makeText(context, R.string.msg_password_null, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    dialog.dismiss();
                    if (app == null) {
                        PREFS_KEY.edit()
                                .putString(Common.KEY_PREFERENCE, shaHash(v1))
                                .commit();
                        PREFS.edit()
                                .putString(Common.LOCKING_TYPE, Common.PREF_VALUE_PASSWORD)
                                .commit();
                    } else {
                        PREFS_PER_APP.edit().putString(app, Common.PREF_VALUE_PASSWORD).putString(app + Common.APP_KEY_PREFERENCE, shaHash(v1)).commit();
                    }
                    Toast.makeText(context, R.string.msg_password_changed, Toast.LENGTH_SHORT)
                            .show();
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
        String toLog = "[" + new SimpleDateFormat("dd/MM/yy, HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + getApplicationNameFromPackage(pkg, context);
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

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static boolean checkInput(String input, String key_type, Context context, String app) {
        System.out.println(app);
        PREFS_KEY = context.getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        String key = PREFS_KEY.getString(key_type, "");
        return key.equals("") || shaHash(input).equals(key);
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

    public static Drawable getApplicationIconFromPackage(String packageName, Context context) {
        PM = context.getApplicationContext().getPackageManager();
        try {
            REQUEST_PKG_INFO = PM.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            REQUEST_PKG_INFO = null;
        }
        return REQUEST_PKG_INFO != null ? PM.getApplicationIcon(REQUEST_PKG_INFO) : context.getResources().getDrawable(R.drawable.ic_launcher);
    }

    /*public static int getTextColor(Context context) {
        if (Integer.valueOf(M_COLOR) == null) {
            Palette p = Palette.generate(getBackground(context));
            M_COLOR = p.getVibrantSwatch().getTitleTextColor();
        }
        return M_COLOR;
    }*/

    private static Bitmap getBackground(Context context) {
        PREFS = PreferenceManager.getDefaultSharedPreferences(context);
        String backgroundType = PREFS.getString(Common.BACKGROUND, "wallpaper");
        switch (backgroundType) {
            case "custom":
                InputStream inputStream;
                try {
                    inputStream = new FileInputStream(new File(context.getApplicationInfo().dataDir + File.separator + "background" + File.separator + "image"));
                    M_BITMAP = BitmapFactory.decodeStream(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "white":
                int[] colors = new int[9000001];
                for (int i = 0; i <= 9000000; i++) {
                    colors[i] = Color.WHITE;
                }
                M_BITMAP = Bitmap.createBitmap(colors, 3000, 3000, Bitmap.Config.ARGB_8888);
                break;
            default:
                Drawable wallpaper = WallpaperManager.getInstance(context).getDrawable();
                M_BITMAP = ((BitmapDrawable) wallpaper).getBitmap();
                break;
        }
        return M_BITMAP;
    }

    public static Drawable getResizedBackground(Context context, int viewWidth, int viewHeight) {
        Bitmap bmp = getBackground(context);
        Bitmap resized;
        if (bmp.getWidth() < viewWidth || bmp.getHeight() < viewHeight) {
            resized = bmp;
        } else {
            try {
                resized = Bitmap.createBitmap(bmp, (bmp.getWidth() - viewWidth) / 2, (bmp.getHeight() - viewHeight) / 2, viewWidth, viewHeight);
            } catch (IllegalArgumentException e) {
                resized = Bitmap.createBitmap(bmp, 0, 0, 1080, 1920);
            }
        }
        return new BitmapDrawable(context.getResources(), resized);
    }

    public static boolean noGingerbread() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1;
    }

    @SuppressLint({"CommitPrefEdits", "WorldReadableFiles"})
    public static void cleanUp(Context context) {
        PREFS = context.getSharedPreferences(Common.PREFS, Context.MODE_PRIVATE);
        PREFS_KEY = context.getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);

        if (!PREFS.getString("migrated", "").equals("3.3")) {

            String lockingType = PREFS.getString(Common.LOCKING_TYPE, "");
            String key;
            switch (lockingType) {
                case Common.PREF_VALUE_PASSWORD:
                    key = PREFS_KEY.getString(Common.PREF_VALUE_PASSWORD, "");
                    break;
                case Common.PREF_VALUE_PIN: {
                    key = PREFS_KEY.getString(Common.PREF_VALUE_PIN, "");
                    break;
                }
                case Common.PREF_VALUE_KNOCK_CODE: {
                    key = PREFS_KEY.getString(Common.PREF_VALUE_KNOCK_CODE, "");
                    break;
                }
                default:
                    key = "";
                    break;
            }
            if (!key.equals(""))
                PREFS_KEY.edit().putString(Common.KEY_PREFERENCE, key).commit();

            PREFS_KEY.edit().remove(Common.PREF_VALUE_PASSWORD).commit();
            PREFS_KEY.edit().remove(Common.PREF_VALUE_PIN).commit();
            PREFS_KEY.edit().remove(Common.PREF_VALUE_KNOCK_CODE).commit();

            String str;
            File file;
            try {
                file = new File(context.getApplicationInfo().dataDir + File.separator + "master_switch");
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                str = bufferedReader.readLine();
            } catch (Exception e) {
                Log.d("MasterSwitch", "File not found!");
                return;
            }
            if (str == null) {
                Log.d("MasterSwitch", "File is empty!");
                str = "1";
            }
            //noinspection deprecation
            context.getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE).edit().putBoolean(Common.MASTER_SWITCH_ON, str.equals("1"));
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            PREFS.edit().putString("migrated", "3.3");
        }
    }
}