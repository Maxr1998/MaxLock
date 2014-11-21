package de.Maxr1998.xposed.maxlock;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.preference.PreferenceManager;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    static AuthenticationSucceededListener authenticationSucceededListener;
    private static int M_COLOR;
    private static Bitmap M_BITMAP;
    private static SharedPreferences PREF, KEYS_PREF;
    private static ApplicationInfo REQUEST_PKG_INFO;
    private static PackageManager PM;

    public static void askPassword(final Context context) {
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(context.getClass().getSimpleName() + "must implement AuthenticationSucceededListener to use this fragment", e);
        }
        final View dialogView = LayoutInflater.from(context).inflate(R.layout.ask_password, null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.locking_type_password)
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
                if (Util.checkInput(val, Common.KEY_PASSWORD, context, context.getPackageName())) {
                    authenticationSucceededListener.onAuthenticationSucceeded();
                    dialog.dismiss();
                } else {
                    input.setText("");
                    Toast.makeText(context, R.string.msg_password_incorrect, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static void setPassword(final Context context) {
        PREF = PreferenceManager.getDefaultSharedPreferences(context);
        KEYS_PREF = context.getSharedPreferences(Common.PREF_KEYS, Activity.MODE_PRIVATE);

        final View dialogView = LayoutInflater.from(context).inflate(R.layout.set_password, null);

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
                    KEYS_PREF.edit()
                            .putString(Common.KEY_PASSWORD, Util.shaHash(v1))
                            .remove(Common.KEY_PIN)
                            .remove(Common.KEY_KNOCK_CODE)
                            .commit();
                    PREF.edit()
                            .putString(Common.LOCKING_TYPE, Common.KEY_PASSWORD)
                            .commit();
                    Toast.makeText(context, R.string.msg_password_changed, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(KEYS_PREF.getString(Common.LOCKING_TYPE, "").equals("pw"))) {
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, R.string.msg_password_null, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    public static String shaHash(String toHash) { // from: [ http://stackoverflow.com/a/11978976 ]. Thanks very much!
        String hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = toHash.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();

            hash = bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
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
        KEYS_PREF = context.getSharedPreferences(Common.PREF_KEYS, Activity.MODE_PRIVATE);
        String key = KEYS_PREF.getString(key_type, "");
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

    public static void getMasterSwitch(Context context) {
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(context.getClass().getSimpleName() + "must implement AuthenticationSucceededListener to use this method", e);
        }
        try {
            File file = new File(context.getApplicationInfo().dataDir + File.separator + "master_switch");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String str = bufferedReader.readLine();
            if (str == null) {
                Log.d("MasterSwitch", "File is empty!");
                return;
            }
            System.out.println(str);
            if (str.equals("0")) {
                authenticationSucceededListener.onAuthenticationSucceeded();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setMasterSwitch(boolean checked, Context context) {
        try {
            PrintWriter writer = new PrintWriter(context.getApplicationInfo().dataDir + File.separator + "master_switch", "UTF-8");
            if (checked) writer.print("1");
            else writer.print("0");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getTextColor(Context context) {
        if (Integer.valueOf(M_COLOR) == null) {
            Palette p = Palette.generate(getBackground(context));
            M_COLOR = p.getVibrantSwatch().getTitleTextColor();
        }
        return M_COLOR;
    }

    private static Bitmap getBackground(Context context) {
        PREF = PreferenceManager.getDefaultSharedPreferences(context);
        String backgroundType = PREF.getString(Common.BACKGROUND, "wallpaper");
        if (backgroundType.equals("custom")) {
            InputStream inputStream;
            try {
                inputStream = new FileInputStream(new File(context.getApplicationInfo().dataDir + File.separator + "background" + File.separator + "image"));
                M_BITMAP = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (backgroundType.equals("white")) {
            int[] colors = new int[9000001];
            for (int i = 0; i <= 9000000; i++) {
                colors[i] = Color.WHITE;
            }
            M_BITMAP = Bitmap.createBitmap(colors, 3000, 3000, Bitmap.Config.ARGB_8888);
        } else {
            Drawable wallpaper = WallpaperManager.getInstance(context).getDrawable();
            M_BITMAP = ((BitmapDrawable) wallpaper).getBitmap();
        }
        return M_BITMAP;
    }

    public static Drawable getResizedBackground(Context context, int viewWidth, int viewHeight) {
        Bitmap bmp = getBackground(context);
        Bitmap resized = Bitmap.createBitmap(bmp, (bmp.getWidth() - viewWidth) / 2, (bmp.getHeight() - viewHeight) / 2, viewWidth, viewHeight);
        return new BitmapDrawable(context.getResources(), resized);
    }
}