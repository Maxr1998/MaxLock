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

package de.Maxr1998.xposed.maxlock;

import android.net.Uri;
import android.os.Environment;

import de.Maxr1998.xposed.maxlock.util.Util;

public abstract class Common {

    public static final String MAXLOCK_PACKAGE_NAME = "de.Maxr1998.xposed.maxlock";

    public static final String INTENT_EXTRAS_NAMES = "intent_extras_pkg_name";
    public static final String INTENT_EXTRAS_CUSTOM_APP = "custom_app";

    // Preference keys/ids //
    public static final String FIRST_START = "first_start";
    public static final String LAST_VERSION_NUMBER = "last_version_number";
    public static final String RATING_DIALOG_APP_OPENING_COUNTER = "rating_dialog_app_opening_counter";
    public static final String RATING_DIALOG_LAST_SHOWN = "rating_dialog_last_shown";
    public static final String RATING_DIALOG_SHOW_NEVER = "rating_dialog_show_never";
    public static final String MASTER_SWITCH_ON = "master_switch_on";
    public static final String KEY_PREFERENCE = "key";
    public static final String APP_KEY_PREFERENCE = "_key";
    public static final String FAILED_ATTEMPTS_COUNTER = "failed_attempts_counter";
    public static final String FAILED_ATTEMPTS_TIMER = "failed_attempts_timer";
    public static final String TASKER_QUERIES = "tasker_queries";
    public static final String THEME_PKG = "theme_pkg";

    public static final String ML_IMPLEMENTATION = "ml_implementation";

    public static final String LOCKING_TYPE_SETTINGS = "locking_type_settings";
    /* TYPE */
    public static final String LOCKING_TYPE = "locking_type";
    public static final String LOCKING_TYPE_PASSWORD = "locking_type_password";
    public static final String LOCKING_TYPE_PIN = "locking_type_pin";
    public static final String LOCKING_TYPE_KNOCK_CODE = "locking_type_knock_code";
    public static final String LOCKING_TYPE_PATTERN = "locking_type_pattern";
    public static final String SHADOW_FINGERPRINT = "shadow_fingerprint";
    public static final String CATEGORY_FINGERPRINT = "cat_fingerprint";
    public static final String DISABLE_FINGERPRINT = "disable_fingerprint";

    public static final String LOCKING_UI_SETTINGS = "locking_ui_settings";
    /* UI */
    //public static final String OPEN_THEME_MANAGER = "open_theme_manager";
    public static final String BACKGROUND = "background";
    public static final String BACKGROUND_COLOR = "background_color";
    public static final String HIDE_STATUS_BAR = "hide_status_bar";
    public static final String INVERT_COLOR = "invert_color";
    public static final String HIDE_TITLE_BAR = "hide_title_bar";
    public static final String ENABLE_QUICK_UNLOCK = "quick_unlock";
    public static final String SHUFFLE_PIN = "shuffle_pin";
    public static final String HIDE_INPUT_BAR = "hide_input_bar";
    public static final String SHOW_KC_DIVIDER = "show_dividers";
    public static final String MAKE_KC_TOUCH_VISIBLE = "touch_visible";
    public static final String SHOW_PATTERN_PATH = "show_path";
    public static final String ENABLE_PATTERN_FEEDBACK = "haptic_feedback";
    public static final String HIDE_FINGERPRINT_ICON = "hide_fingerprint_icon";

    public static final String LOCKING_OPTIONS = "locking_options";
    /* OPTIONS */
    public static final String ENABLE_LOGGING = "enable_logging";
    public static final String VIEW_LOGS = "view_logs";
    public static final String LOG_FILE = "log.txt";
    public static final String ENABLE_FAKE_CRASH_ALL_APPS = "fake_crash_all_apps";
    public static final String ENABLE_DIRECT_UNLOCK = "enable_direct_unlock";
    public static final String FAKE_CRASH_INPUT = "fake_die_input";
    public static final String ENABLE_TASKER = "enable_tasker_integration";

    public static final String IMOD_OPTIONS = "intika_imod";
    /* IMOD */
    public static final String ENABLE_IMOD_DELAY_GLOBAL = "enable_delaygeneral";
    public static final String ENABLE_IMOD_DELAY_APP = "enable_delayperapp";
    public static final String SHOW_IMOD_RESET_NOTIFICATION = "imod_show_reset_notification";

    public static final String CHOOSE_APPS = "choose_apps";

    public static final String CATEGORY_APPLICATION_UI = "cat_app_ui";
    public static final String HIDE_APP_FROM_LAUNCHER = "hide_from_launcher";
    public static final String USE_DARK_STYLE = "use_dark_style";
    public static final String USE_AMOLED_BLACK = "use_amoled_black";
    public static final String NEW_APP_NOTIFICATION = "new_app_notification";

    public static final String ABOUT = "about";
    /* ABOUT */
    public static final String SHOW_CHANGELOG = "show_changelog";
    public static final String VISIT_WEBSITE = "visit_website";
    public static final String TECHNOSPARKS_PROFILE = "technosparks_profile";

    public static final String DONATE = "donate_upgrade_pro";
    public static final String DONATED = "donated";
    public static final String ENABLE_PRO = "enable_pro";
    public static final String UNINSTALL = "uninstall";
    public static final String SEND_FEEDBACK = "send_feedback";

    // Preference values //
    public static final String PREF_VALUE_PASSWORD = "password";
    public static final String PREF_VALUE_PASS_PIN = "pass_pin";
    public static final String PREF_VALUE_PIN = "pin";
    public static final String PREF_VALUE_KNOCK_CODE = "knock_code";
    public static final String PREF_VALUE_PATTERN = "pattern";

    // Preference files //
    public static final String PREFS_KEY = "keys";
    public static final String PREFS_APPS = "packages";
    public static final String PREFS_KEYS_PER_APP = "per_app_settings";
    //public static final String PREFS_THEME = "theme";

    // Modes //
    public static final String LOCK_ACTIVITY_MODE = "la_mode";
    public static final String MODE_FAKE_CRASH = "fake-crash";
    public static final String MODE_UNLOCK = "force-unlock";

    // Directories //
    public static final String EXTERNAL_FILES_DIR = Environment.getExternalStorageDirectory() + "/MaxLock/";
    public static final String BACKUP_DIR = EXTERNAL_FILES_DIR + "Backup/";

    // URLS //
    public static final Uri WEBSITE_URI = Uri.parse("https://maxlock.maxr1998.de/?client=inapp&lang=" + Util.getLanguageCode());
    public static final Uri MAXR1998_URI = Uri.parse("https://maxr1998.de/");
    public static final Uri PAYPAL_DONATE_URI = Uri.parse("https://maxr1998.de/r/donate");
    public static final Uri TECHNO_SPARKS_URI = Uri.parse("https://www.technosparks.net/");

    public static final Uri KNOWN_PROBLEM_SETTINGS_URI = Uri.parse("https://maxlock.maxr1998.de/pages/general/known-problems#Incompatibility");
}