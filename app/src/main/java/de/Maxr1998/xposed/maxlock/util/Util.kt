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

package de.Maxr1998.xposed.maxlock.util

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.support.v4.content.AsyncTaskLoader
import de.Maxr1998.xposed.maxlock.util.Util.PATTERN_CODE
import de.Maxr1998.xposed.maxlock.util.Util.PATTERN_CODE_APP
import java.lang.ref.SoftReference

object KUtil {
    @JvmStatic
    fun getLauncherPackages(pm: PackageManager): List<String> {
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
                .mapTo(ArrayList<String>(), { it.activityInfo.packageName }).apply { remove("com.android.settings") }
    }

    @JvmStatic
    fun getPatternCode(app: Int): Int = if (app == -1) PATTERN_CODE else (PATTERN_CODE_APP.toString() + app.toString()).toInt()
}

private var WALLPAPER = SoftReference<Drawable>(null)

class WallpaperDrawableLoader(context: Context) : AsyncTaskLoader<Drawable>(context) {
    override fun onStartLoading() {
        super.onStartLoading()
        forceLoad()
    }

    override fun loadInBackground(): Drawable {
        return WALLPAPER.get() ?: WallpaperManager.getInstance(context).fastDrawable.also { WALLPAPER = SoftReference(it) }
    }
}