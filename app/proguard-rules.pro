-dontwarn **

-keep class de.Maxr1998.xposed.maxlock.hooks.** {*;}

-keep class de.Maxr1998.xposed.maxlock.ui.actions.ActionsHelper {
    public static void clearImod();
}

-keepclassmembers class ** {
    void onAuthenticationSucceeded();
}

-keepclassmembers class  com.haibison.android.lockpattern.widget.LockPatternView {
    private int mRegularColor;
    private int mSuccessColor;
}

-keep class android.support.v7.widget.SearchView { *; }