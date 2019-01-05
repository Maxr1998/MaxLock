/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2018  Max Rumpf alias Maxr1998
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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;

import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.XC_MethodHook;

import static android.os.Build.VERSION.SDK_INT;
import static de.Maxr1998.xposed.maxlock.Common.MAXLOCK_PACKAGE_NAME;
import static de.Maxr1998.xposed.maxlock.hooks.Main.logD;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

class NotificationHiding {
    static void init(final String packageName, final Context appContext, final SharedPreferences prefsApps) {
        if (!prefsApps.getBoolean(packageName, false))
            return;
        try {
            final Resources modRes = appContext.getPackageManager().getResourcesForApplication(MAXLOCK_PACKAGE_NAME);
            final String replacementText = modRes.getString(modRes.getIdentifier("notification_hidden_by_maxlock", "string", MAXLOCK_PACKAGE_NAME));
            final int mlColor = modRes.getColor(modRes.getIdentifier("primary_red", "color", MAXLOCK_PACKAGE_NAME));

            // Notification (content) hiding
            findAndHookMethod(NotificationManager.class, "notify", String.class, int.class, Notification.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)) {
                        if (prefsApps.getBoolean(packageName + Common.APP_HIDE_NOTIFICATIONS_PREFERENCE, false)) {
                            logD("Blocked notification from " + packageName);
                            param.setResult(null);
                        } else if (prefsApps.getBoolean(packageName + Common.APP_HIDE_NOTIFICATION_CONTENT_PREFERENCE, false)) {
                            logD("Hiding notification content for " + packageName);
                            Notification notification = (Notification) param.args[2];
                            String appName = appContext.getPackageManager().getApplicationInfo(packageName, 0).loadLabel(appContext.getPackageManager()).toString();
                            Notification.Builder replacementBuilder = (SDK_INT > Build.VERSION_CODES.O ?
                                    new Notification.Builder(appContext, notification.getChannelId()) : new Notification.Builder(appContext))
                                    .setLights(notification.ledARGB, notification.ledOnMS, notification.ledOffMS)
                                    .setSound(notification.sound)
                                    .setContentIntent(notification.contentIntent)
                                    .setDeleteIntent(notification.deleteIntent)
                                    .setWhen(notification.when);
                            if (SDK_INT >= Build.VERSION_CODES.N) {
                                replacementBuilder.setContentTitle(replacementText);
                            } else replacementBuilder.setContentTitle(appName)
                                    .setContentText(replacementText);
                            replacementBuilder.setColor(mlColor)
                                    .setGroup(notification.getGroup())
                                    .setSortKey(notification.getSortKey())
                                    .setSound(notification.sound, notification.audioAttributes)
                                    .setVisibility(Notification.VISIBILITY_SECRET);
                            if (SDK_INT >= Build.VERSION_CODES.M)
                                replacementBuilder.setSmallIcon(notification.getSmallIcon());
                            if (SDK_INT >= Build.VERSION_CODES.O)
                                replacementBuilder.setGroupAlertBehavior(notification.getGroupAlertBehavior());
                            Notification replacement = replacementBuilder.build();
                            replacement.flags = notification.flags;
                            // Replace notification
                            param.args[2] = replacement;
                        }
                    }
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }
}