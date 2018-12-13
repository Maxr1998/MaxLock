-dontobfuscate

-keepclassmembers class ** {
    void <init>(android.content.Context);
}

-assumevalues class android.os.Build$VERSION {
    int SDK_INT return 21..2147483647;
}