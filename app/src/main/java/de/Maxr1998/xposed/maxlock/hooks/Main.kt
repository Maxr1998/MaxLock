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

package de.Maxr1998.xposed.maxlock.hooks

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.crossbowffs.remotepreferences.RemotePreferences
import de.Maxr1998.xposed.maxlock.BuildConfig
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.Common.PREFS_APPS
import de.Maxr1998.xposed.maxlock.Common.PREFS_HISTORY
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class Main : IXposedHookZygoteInit, IXposedHookLoadPackage {

    @Throws(Throwable::class)
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        log("ML: Loaded Main class")
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lPParam: LoadPackageParam) {
        findAndHookMethod(Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val ctx = param.args[0] as Context
                val prefsApps = ctx.getRemotePreferences(PREFS_APPS)
                val prefsHistory = ctx.getRemotePreferences(PREFS_HISTORY)
                when (lPParam.packageName) {
                    SystemUI.PACKAGE_NAME -> {
                        // Enable MasterSwitch on boot
                        prefsApps?.edit { putBoolean(Common.MASTER_SWITCH_ON, true) }
                        // Clear delayed relock on boot
                        prefsHistory?.edit { clear() }

                        val prefs = ctx.getRemotePreferences(Common.MAXLOCK_PACKAGE_NAME + "_preferences")
                        SystemUI.init(lPParam, prefs, prefsApps, prefsHistory)
                        SystemUI.initScreenOff(lPParam, prefsApps, prefsHistory)
                        return
                    }
                    MaxLock.PACKAGE_NAME -> MaxLock.init(lPParam, prefsHistory)
                    DeviceAdminProtection.PACKAGE_NAME -> DeviceAdminProtection.init(lPParam)
                }
                Apps.init(lPParam.packageName, ctx, prefsApps, prefsHistory)
            }
        })
    }

    companion object {
        private fun Context.getRemotePreferences(name: String): SharedPreferences? =
                RemotePreferences(this, Common.PREFERENCE_PROVIDER_AUTHORITY, name)

        @JvmStatic
        fun logD(message: String) {
            if (BuildConfig.DEBUG)
                log("ML: $message")
        }
    }
}