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

package de.Maxr1998.xposed.maxlock.util

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.Common.SETTINGS_PACKAGE_NAME
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.Util.PATTERN_CODE
import de.Maxr1998.xposed.maxlock.util.Util.PATTERN_CODE_APP

val Context.applicationName
    get() = StringBuilder(getString(R.string.app_name)).apply {
        if (Util.isDevMode()) {
            append(" Indev")
        } else if (prefs.getBoolean(Common.ENABLE_PRO, false)) {
            append(" ").append(getString(if (prefs.getBoolean(Common.DONATED, false)) R.string.name_pro else R.string.name_pseudo_pro))
        }
    }

inline val AndroidViewModel.application
    get() = getApplication<Application>()

fun ViewGroup.inflate(id: Int, attachToRoot: Boolean = false): View =
        LayoutInflater.from(context).inflate(id, this, attachToRoot)

fun Context.withAttrs(vararg attrs: Int, block: TypedArray.() -> Unit) {
    obtainStyledAttributes(attrs).apply {
        block()
        recycle()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Context.getColorCompat(@ColorRes id: Int) = ContextCompat.getColor(this, id)

fun AlertDialog.showWithLifecycle(fragment: Fragment) {
    val observer = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun paused() {
            dismiss()
        }
    }
    setOnDismissListener { fragment.lifecycle.removeObserver(observer) }
    show()
    fragment.lifecycle.addObserver(observer)
}

object KUtil {
    @JvmStatic
    fun getLauncherPackages(pm: PackageManager): List<String> {
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
                .mapTo(ArrayList<String>()) { it.activityInfo.packageName }.apply { remove(SETTINGS_PACKAGE_NAME) }
    }

    @JvmStatic
    fun getPatternCode(app: Int): Int = if (app == -1) PATTERN_CODE else (PATTERN_CODE_APP.toString() + app.toString()).toInt()
}