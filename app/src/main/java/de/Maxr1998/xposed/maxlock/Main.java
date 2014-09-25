package de.Maxr1998.xposed.maxlock;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    public static final String MY_PACKAGE_NAME = Main.class.getPackage().getName();
    private static XSharedPreferences pref, packagePref;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        pref = new XSharedPreferences(MY_PACKAGE_NAME);
        packagePref = new XSharedPreferences(MY_PACKAGE_NAME, Common.PREF_PACKAGE);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        pref.reload();
        packagePref.reload();

        final String packageName = lpparam.packageName;

        if (!packagePref.getBoolean(packageName, false) || !pref.getBoolean("master_switch", true)) {
            return;
        }

        Long timestamp = System.currentTimeMillis();
        Long permitTimestamp = packagePref.getLong(packageName + "_tmp", 0);
        if (permitTimestamp != 0 && timestamp - permitTimestamp <= 10000) {
            return;
        }

        Class<?> activity = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader);
        XposedBridge.hookAllMethods(activity, "onCreate", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);

                        final Activity app = (Activity) param.thisObject;

                        if (!app.getClass().getName().equals("android.app.Activity") && !packagePref.getBoolean(packageName + "_fake", false)) {
                            launchLockActivity(app, packageName);
                        }

                        if (packagePref.getBoolean(packageName + "_fake", false)) {
                            launchFakeDieDialog(app, packageName);
                        }

                        app.setResult(Activity.RESULT_CANCELED);
                        app.finish();

                        param.setResult(new Object());

                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }
        );
    }

    private void launchLockActivity(final Activity app, String packageName) {
        Intent it = new Intent();
        it.setComponent(new ComponentName(MY_PACKAGE_NAME, MY_PACKAGE_NAME + ".ui.LockActivity"));
        it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        it.putExtra(Common.KEY_APP_ACCESS, packageName);
        app.startActivity(it);
    }

    private void launchFakeDieDialog(final Activity app, String packageName) {
        Intent dialog = new Intent();
        dialog.setComponent(new ComponentName(MY_PACKAGE_NAME, MY_PACKAGE_NAME + ".ui.FakeDieDialog"));
        dialog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        dialog.putExtra(Common.KEY_APP_ACCESS, packageName);
        app.startActivity(dialog);
    }
}
