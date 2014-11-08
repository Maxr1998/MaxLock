package de.Maxr1998.xposed.maxlock.lib;

/* Status Bar Tinting API v1
 * (C) 2013 Mohammad Abu-Garbeyyeh
 * Feel free to copy this class into your project as is, just change the package declaration above.
 */

import android.content.Context;
import android.content.Intent;

@SuppressWarnings("unused")
public class StatusBarTintApi {
    public static final String INTENT_CHANGE_COLOR_NAME = "com.mohammadag.colouredstatusbar.ChangeStatusBarColor";

    public static final String KEY_STATUS_BAR_TINT = "status_bar_color";
    public static final String KEY_STATUS_BAR_ICON_TINT = "status_bar_icons_color";
    public static final String KEY_NAVIGATION_BAR_TINT = "navigation_bar_color";
    public static final String KEY_NAVIGATION_BAR_ICON_TINT = "navigation_bar_icon_tint";

    /*
     * You can use this meta-data value to override auto detection of colours.
     * <meta-data android:name="override_tinted_status_bar_defaults" android:value="true" />
     *
     * You should implement colour changes by sending an intent from the onResume() method of
     * each Activity.
     *
     * Here's an example on how to do that (helper method below)
     *     int color = Color.parseColor("#33b5e5");
     *     Intent intent = new Intent("com.mohammadag.colouredstatusbar.ChangeStatusBarColor");
     *     intent.putExtra("status_bar_color", color);
     *     intent.putExtra("status_bar_icons_color", Color.WHITE);
     *     // Please note that these are not yet implemented!!!
     *     // You're free to include them in your code so that when they
     *     // are implemented, your app will work out of the box.
     *     intent.putExtra("navigation_bar_color", Color.BLACK);
     *     intent.putExtra("navigation_bar_icon_color", Color.WHITE);
     *     context.sendOrderedBroadcast(intent, null);
     */
    public static final String METADATA_OVERRIDE_COLORS = "override_tinted_status_bar_defaults";

    /*
     * You can use this meta-data value to provide custom made plugins for com.packagename
     * <meta-data android:name="tinted_status_bar_plugin" android:value="com.packagename" />
     *
     * For multiple packages, separate names with a #
     * <meta-data android:name="tinted_status_bar_plugin" android:value="com.packagename1#com.packagename2" />
     */
    public static final String METADATA_PLUGIN = "tinted_status_bar_plugin";

    /* Helper method, pass -3 for a colour you don't want to change */
    public static void sendColorChangeIntent(int statusBarTint, int iconColorTint,
                                             int navBarTint, int navBarIconTint, Context context) {
        Intent intent = new Intent(INTENT_CHANGE_COLOR_NAME);
        if (statusBarTint != -3)
            intent.putExtra(KEY_STATUS_BAR_TINT, statusBarTint);
        if (iconColorTint != -3)
            intent.putExtra(KEY_STATUS_BAR_ICON_TINT, iconColorTint);
        if (navBarTint != -3)
            intent.putExtra(KEY_NAVIGATION_BAR_TINT, navBarTint);
        if (navBarIconTint != -3)
            intent.putExtra(KEY_NAVIGATION_BAR_ICON_TINT, navBarIconTint);

		/* Used internally to keep track of delayed intents */
        intent.putExtra("time", System.currentTimeMillis());
        context.sendBroadcast(intent);
    }
}
