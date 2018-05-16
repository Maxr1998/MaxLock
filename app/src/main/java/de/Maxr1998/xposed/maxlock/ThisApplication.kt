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

package de.Maxr1998.xposed.maxlock

import android.app.Application
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker
import de.Maxr1998.xposed.maxlock.util.Util
import java.io.File

class ThisApplication : Application() {
    private lateinit var tracker: Tracker

    override fun onCreate() {
        super.onCreate()
        if (!Util.isDevMode() || !File(Common.EXTERNAL_FILES_DIR, "disable-ga").exists()) {
            tracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.app_tracker)
            tracker.setAnonymizeIp(true)
        }
    }
}