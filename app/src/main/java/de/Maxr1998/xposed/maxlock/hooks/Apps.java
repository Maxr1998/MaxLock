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

package de.Maxr1998.xposed.maxlock.hooks;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;

import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.Maxr1998.xposed.maxlock.Common.MAXLOCK_PACKAGE_NAME;
import static de.Maxr1998.xposed.maxlock.hooks.Main.logD;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.addToHistory;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.pass;
import static de.Maxr1998.xposed.maxlock.util.AppLockHelpers.wasAppClosed;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

class Apps {

    static void initLogging(final XC_LoadPackage.LoadPackageParam lPParam, final SharedPreferences prefsHistory) {
        try {
            findAndHookMethod("android.app.Activity", lPParam.classLoader, "onStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    addToHistory(((Activity) param.thisObject).getTaskId(), lPParam.packageName, prefsHistory);
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }

    static void init(final XC_LoadPackage.LoadPackageParam lPParam, final SharedPreferences prefsApps, final SharedPreferences prefsHistory) {
        try {
            findAndHookMethod(Activity.class, "onStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Activity activity = (Activity) param.thisObject;
                    String activityName = activity.getClass().getName();
                    logD("ML|Started " + activityName + " at " + System.currentTimeMillis());
                    if (wasAppClosed(lPParam.packageName, prefsHistory)) {
                        activity.finish();
                        logD("ML|Finish " + activityName);
                        return;
                    }
                    if (activityName.equals("android.app.Activity") ||
                            pass(activity.getTaskId(), lPParam.packageName, activityName, prefsApps, prefsHistory)) {
                        return;
                    }
                    Intent i = new Intent()
                            .setComponent(new ComponentName(MAXLOCK_PACKAGE_NAME, MAXLOCK_PACKAGE_NAME + ".ui.LockActivity"))
                            .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            .putExtra(Common.INTENT_EXTRAS_NAMES, new String[]{lPParam.packageName, activityName});
                    if (prefsApps.getBoolean(lPParam.packageName + "_fake", false)) {
                        i.putExtra(Common.LOCK_ACTIVITY_MODE, Common.MODE_FAKE_CRASH);
                    }
                    activity.startActivity(i);
                }
            });
            findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((boolean) param.args[0]) {
                        if (wasAppClosed(lPParam.packageName, prefsHistory)) {
                            final Activity activity = (Activity) param.thisObject;
                            activity.finish();
                            logD("ML|Finish " + activity.getClass().getName());
                        }
                    }
                }
            });

            // Notification content hiding
            findAndHookMethod(NotificationManager.class, "notify", String.class, int.class, Notification.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Notification notification = (Notification) param.args[2];
                    if (prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true) && prefsApps.getBoolean(lPParam.packageName + "_notif_content", false)) {
                        Context context = (Context) getObjectField(param.thisObject, "mContext");
                        String appName = context.getPackageManager().getApplicationInfo(lPParam.packageName, 0).loadLabel(context.getPackageManager()).toString();
                        Resources modRes = context.getPackageManager().getResourcesForApplication(MAXLOCK_PACKAGE_NAME);
                        String replacement = modRes.getString(modRes.getIdentifier("notification_hidden_by_maxlock", "string", MAXLOCK_PACKAGE_NAME));
                        Notification.Builder b = new Notification.Builder(context).setContentTitle(appName).setContentText(replacement);
                        notification.contentView = b.build().contentView;
                        notification.bigContentView = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            notification.headsUpContentView = null;
                        notification.tickerText = replacement;
                    }
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }
}