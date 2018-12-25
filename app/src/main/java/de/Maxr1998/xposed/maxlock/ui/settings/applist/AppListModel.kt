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

package de.Maxr1998.xposed.maxlock.ui.settings.applist

import android.app.ActivityManager
import android.app.Application
import android.content.Context.ACTIVITY_SERVICE
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.LruCache
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.SortedList
import de.Maxr1998.xposed.maxlock.BuildConfig
import de.Maxr1998.xposed.maxlock.util.GenericEventLiveData
import de.Maxr1998.xposed.maxlock.util.KUtil.getLauncherPackages
import kotlinx.coroutines.*
import java.text.Collator
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class AppListModel(application: Application) : AndroidViewModel(application), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    val adapter: AppListAdapter by lazy { AppListAdapter(this, application) }
    private val appListCallback = AppListCallback(adapter)
    val appList = SortedList(AppInfo::class.java, appListCallback)
    val appListBackup = ArrayList<AppInfo>()
    val appsLoadedListener = GenericEventLiveData<Any?>()
    val dialogDispatcher = GenericEventLiveData<AlertDialog?>()
    val fragmentFunctionDispatcher = MutableLiveData<(Fragment.() -> Any)?>()
    var loadAll = false
    val iconCache = LruCache<String, Drawable>(
            if ((application.getSystemService(ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice) 80 else 300
    )
    private lateinit var launcherPackages: List<String>
    val loaded = AtomicBoolean(false)

    init {
        loadData()
    }

    fun loadData() {
        loaded.set(false)
        launch {
            withContext(AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()) {
                val pm = getApplication<Application>().packageManager
                launcherPackages = getLauncherPackages(pm)
                val allApps = pm.getInstalledApplications(0)
                val result = ArrayList<AppInfo>(allApps.size * 3 / 4)
                for (i in allApps.indices) {
                    val info = allApps[i]
                    if (includeApp(pm, info.packageName)) {
                        result.add(AppInfo(i, info, pm))
                    }
                }
                val collator = Collator.getInstance()
                result.sortWith(Comparator { a, b ->
                    compareValuesBy(a, b, collator, AppInfo::name)
                })
                result
            }.let {
                appListBackup.clear()
                appListBackup.addAll(it)
                appsLoadedListener.call(null)
                appList.replaceAll(it)
                loaded.set(true)
            }
        }
    }

    private fun includeApp(pm: PackageManager, packageName: String): Boolean {
        return when {
            packageName == BuildConfig.APPLICATION_ID -> false
            launcherPackages.contains(packageName) -> false
            packageName.matches(Regex("com.(google.)?android.packageinstaller")) -> true
            loadAll && pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities != null -> true
            else -> pm.getLaunchIntentForPackage(packageName) != null
        }
    }

    data class AppInfo(val id: Int, private val appInfo: ApplicationInfo, private val pm: PackageManager) {
        val packageName: String = appInfo.packageName
        val name: String by lazy { appInfo.loadLabel(pm).toString() }

        fun loadIcon(): Drawable = appInfo.loadIcon(pm)
    }
}