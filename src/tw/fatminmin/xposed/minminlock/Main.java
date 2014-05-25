package tw.fatminmin.xposed.minminlock;

import android.app.Activity;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    
    public static final String MY_PACKAGE_NAME = Main.class.getPackage().getName();
    private static XSharedPreferences pref;
    
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        pref = new XSharedPreferences(MY_PACKAGE_NAME);
    }
    
    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        
        pref.reload();
        
        if(pref.getBoolean(lpparam.packageName, false)) {
            Class<?> activity = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader);
            XposedBridge.hookAllMethods(activity, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Activity app = (Activity) param.thisObject;
                    app.finish();
                }
            });
        }
    }
}
