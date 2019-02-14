-dontwarn **

-keepclassmembers class ** {
    void <init>(android.content.Context);
}

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

-assumevalues class android.os.Build$VERSION {
    int SDK_INT return 21..2147483647;
}

-assumevalues class de.Maxr1998.xposed.maxlock.BuildConfig {
    boolean DEBUG return false;
}