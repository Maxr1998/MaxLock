package de.Maxr1998.xposed.maxlock;

public class Common {
    public static final String PKG_NAME = "de.Maxr1998.xposed.maxlock";
    public static final String ACTION_APP_LOADED = PKG_NAME + ".APP_LOADED";

    public static final String KEY_APP_ACCESS = "app_access";


    // Preferences
    public static final String MASTER_SWITCH = "master_switch";

    public static final String LOCK_TYPE = "lock_type";

    public static final String LOCK_TYPE_PASSWORD = "lock_type_password";
    public static final String LOCK_TYPE_PIN = "lock_type_pin";
    public static final String LOCK_TYPE_KNOCK_CODE = "lock_type_knock_code";

    public static final String KC_HIDE_DIVIDERS = "kc_hide_dividers";
    public static final String KC_NO_HIGHLIGHT = "kc_no_highlight";

    public static final String CHOOSE_APPS = "choose_apps";

    public static final String HIDE_APP_FROM_LAUNCHER = "hide_from_launcher";
    public static final String USE_DARK_STYLE = "use_dark_style";

    // Preference values
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PIN = "pin";
    public static final String KEY_KNOCK_CODE = "knock_code";

    // Preference files
    public static final String PREF = "de.Maxr1998.xposed.maxlock_preferences";
    public static final String PREF_KEYS = "keys";
    public static final String PREF_PACKAGE = "packages";

    // Fragment tags
    public static final String TAG_KCF = "knock_code_fragment";

    public static String REQUEST_PKG = "";

}
