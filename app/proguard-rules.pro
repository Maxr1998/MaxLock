-dontwarn **

-keep class de.Maxr1998.xposed.maxlock.hooks.** {*;}

-keep class de.Maxr1998.xposed.maxlock.ui.actions.ActionsHelper {
    public static void clearImod();
}

-keepclassmembers class ** {
    void onAuthenticationSucceeded();
}

-keepclassmembers class de.Maxr1998.xposed.maxlock.ui.LockActivity {
    private *** names;
}

-keepclassmembers class de.Maxr1998.xposed.maxlock.ui.SettingsActivity {
    private static boolean IS_ACTIVE;
}

-keepclassmembers class  com.haibison.android.lockpattern.widget.LockPatternView {
    private int mRegularColor;
    private int mSuccessColor;
}