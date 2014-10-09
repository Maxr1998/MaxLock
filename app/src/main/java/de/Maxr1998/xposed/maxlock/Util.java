package de.Maxr1998.xposed.maxlock;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    static SharedPreferences PREF, KEYS_PREF;
    private static ApplicationInfo REQUEST_PKG_INFO;
    private static PackageManager PM;

    public static String sha1Hash(String toHash) { // from: [ http://stackoverflow.com/a/11978976 ]. Thanks very much!
        String hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
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

    public static boolean checkInput(String input, String key_type, Context context) {
        KEYS_PREF = context.getSharedPreferences(Common.PREF_KEYS, Activity.MODE_WORLD_READABLE);
        String key = KEYS_PREF.getString(key_type, "");
        return sha1Hash(input).equals(key);
    }

    public static Drawable getBackground(Context context) {
        PREF = context.getSharedPreferences(Common.PREF, Activity.MODE_WORLD_READABLE);
        Drawable background;
        String backgroundType = PREF.getString(Common.KC_BACKGROUND, "wallpaper");

        if (backgroundType.equals("wallpaper"))
            background = WallpaperManager.getInstance(context).getDrawable();
        else if (backgroundType.equals("white"))
            background = context.getResources().getDrawable(R.color.white);
        else background = WallpaperManager.getInstance(context).getDrawable();

        return background;
    }

    public static String getApplicationNameFromPackage(String packageName, Context context) {
        PM = context.getApplicationContext().getPackageManager();
        try {
            REQUEST_PKG_INFO = PM.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            REQUEST_PKG_INFO = null;
        }
        return (String) (REQUEST_PKG_INFO != null ? PM.getApplicationLabel(REQUEST_PKG_INFO) : "(unknown)");
    }

    public static Drawable getApplicationIconFromPackage(String packageName, Context context) {
        PM = context.getApplicationContext().getPackageManager();
        try {
            REQUEST_PKG_INFO = PM.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            REQUEST_PKG_INFO = null;
        }
        return PM.getApplicationIcon(REQUEST_PKG_INFO);
    }
}