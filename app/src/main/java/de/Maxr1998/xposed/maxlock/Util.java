package de.Maxr1998.xposed.maxlock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static SharedPreferences keysPref;

    public static String sha1Hash(String toHash) // from: [ http://stackoverflow.com/a/11978976 ]. Thanks very much!
    {
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
        keysPref = context.getSharedPreferences(Common.PREF_KEYS, Activity.MODE_WORLD_READABLE);

        String key = keysPref.getString(key_type, "");

        return sha1Hash(input).equals(key);
    }
}