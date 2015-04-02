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

package de.Maxr1998.xposed.maxlock;

public class Common {

    public static final String PKG_NAME = "de.Maxr1998.xposed.maxlock";

    public static final String INTENT_EXTRAS_PKG_NAME = "intent_extras_pkg_name";
    public static final String INTENT_EXTRAS_INTENT = "intent_extras_intent";
    public static final String INTENT_EXTRAS_CUSTOM_APP = "custom_app";

    // Preference keys/ids
    public static final String FIRST_START = "first_start";
    public static final String FIRST_START_TIME = "first_start_time";
    public static final String DIALOG_SHOW_NEVER = "dialog_show_never";
    public static final String THEME_PKG = "theme_pkg";

    public static final String LOCKING_TYPE_SETTINGS = "locking_type_settings";

    public static final String LOCKING_TYPE = "locking_type";
    public static final String LOCKING_TYPE_PASSWORD = "locking_type_password";
    public static final String LOCKING_TYPE_PIN = "locking_type_pin";
    public static final String LOCKING_TYPE_KNOCK_CODE = "locking_type_knock_code";
    public static final String LOCKING_TYPE_PATTERN = "locking_type_pattern";

    public static final String LOCKING_UI_SETTINGS = "locking_ui_settings";

    public static final String OPEN_THEME_MANAGER = "open_theme_manager";
    public static final String BACKGROUND = "background";
    public static final String BACKGROUND_COLOR = "background_color";
    public static final String INVERT_COLOR = "invert_color";
    public static final String HIDE_TITLE_BAR = "hide_title_bar";
    public static final String HIDE_INPUT_BAR = "hide_input_bar";
    public static final String QUICK_UNLOCK = "quick_unlock";
    public static final String KC_SHOW_DIVIDERS = "show_dividers";
    public static final String KC_TOUCH_VISIBLE = "touch_visible";
    public static final String PATTERN_SHOW_PATH = "show_path";
    public static final String PATTERN_FEEDBACK = "haptic_feedback";

    public static final String LOCKING_OPTIONS = "locking_options";

    public static final String ENABLE_LOGGING = "enable_logging";
    public static final String VIEW_LOGS = "view_logs";
    public static final String LOG_FILE = "log.txt";

    public static final String CHOOSE_APPS = "choose_apps";

    public static final String HIDE_APP_FROM_LAUNCHER = "hide_from_launcher";
    public static final String USE_DARK_STYLE = "use_dark_style";
    public static final String FAKE_DIE_INPUT = "fake_die_input";

    public static final String ABOUT = "about";
    public static final String DONATE = "donate_upgrade_pro";
    public static final String ENABLE_PRO = "enable_pro";
    public static final String UNINSTALL = "uninstall";

    public static final String MASTER_SWITCH_ON = "master_switch_on";
    public static final String KEY_PREFERENCE = "key";
    public static final String APP_KEY_PREFERENCE = "_key";

    // Preference values
    public static final String PREF_VALUE_PASSWORD = "password";
    public static final String PREF_VALUE_PASS_PIN = "pass_pin";
    public static final String PREF_VALUE_PIN = "pin";
    public static final String PREF_VALUE_KNOCK_CODE = "knock_code";
    public static final String PREF_VALUE_PATTERN = "pattern";

    // Preference files
    public static final String PREFS = "de.Maxr1998.xposed.maxlock_preferences";
    public static final String PREFS_KEY = "keys";
    public static final String PREFS_PACKAGES = "packages";
    public static final String PREFS_PER_APP = "per_app_settings";
    public static final String PREFS_ACTIVITIES = "activities";
    public static final String PREFS_THEME = "theme";

    // Intika I.MoD
    public static final String IIMOD_OPTIONS = "intika_imod";
    public static final String IMOD_DELAY_GLOBAL_ENABLED = "enable_delaygeneral";
    public static final String IMOD_DELAY_APP_ENABLED = "enable_delayperapp";
    public static final String IMOD_DELAY_APP = "delay_inputperapp";
    public static final String IMOD_DELAY_GLOBAL = "delay_inputgeneral";
    public static final String IMOD_REMAIN_TIMER_GLOBAL = "timer_generaldelay";
    public static final String IMOD_LAST_UNLOCK_GLOBAL = "IMoDGlobalDelayTimer";
    public static final String IMOD_MIN_FAKE_UNLOCK = "enable_minfakelock";
}