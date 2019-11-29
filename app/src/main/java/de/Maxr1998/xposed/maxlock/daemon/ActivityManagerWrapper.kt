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

package de.Maxr1998.xposed.maxlock.daemon

import android.app.IActivityManager
import android.content.IContentProvider
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.Q
import android.os.Bundle
import android.os.IBinder
import android_hidden.app.ActivityManager
import androidx.annotation.RequiresApi


sealed class ActivityManagerWrapper {
    val activityManager: IActivityManager = ActivityManager.getService()

    abstract fun startActivity(intent: Intent, resultWho: String, requestCode: Int, flags: Int, options: Bundle): Int

    abstract fun getContentProvider(name: String, userId: Int, token: IBinder, tag: String): IContentProvider
    abstract fun getForegroundStackInfo(): ActivityManager.StackInfo

    companion object {
        fun get(): ActivityManagerWrapper = when {
            SDK_INT >= Q -> ActivityManagerWrapperApi29()
            SDK_INT >= P -> ActivityManagerWrapperApi28()
            else -> ActivityManagerWrapperBase()
        }
    }
}

open class ActivityManagerWrapperBase : ActivityManagerWrapper() {
    override fun startActivity(intent: Intent, resultWho: String, requestCode: Int, flags: Int, options: Bundle): Int =
            activityManager.startActivity(null, null, intent, null, null, resultWho, requestCode, flags, null, options)

    override fun getContentProvider(name: String, userId: Int, token: IBinder, tag: String): IContentProvider =
            activityManager.getContentProviderExternal(name, userId, token).provider

    override fun getForegroundStackInfo(): ActivityManager.StackInfo =
            activityManager.getStackInfo(activityManager.focusedStackId)
}

@RequiresApi(P)
open class ActivityManagerWrapperApi28 : ActivityManagerWrapperBase() {
    override fun getForegroundStackInfo(): ActivityManager.StackInfo =
            activityManager.focusedStackInfo
}

@RequiresApi(Q)
open class ActivityManagerWrapperApi29 : ActivityManagerWrapperApi28() {
    override fun getContentProvider(name: String, userId: Int, token: IBinder, tag: String): IContentProvider =
            activityManager.getContentProviderExternal(name, userId, token, tag).provider
}