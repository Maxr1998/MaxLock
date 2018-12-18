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

package de.Maxr1998.xposed.maxlock.ui.settings_new

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
import android.widget.ProgressBar
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.recyclerview.widget.RecyclerView
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.Common.*
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity
import de.Maxr1998.xposed.maxlock.ui.lockscreen.LockView
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListAdapter
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListModel
import de.Maxr1998.xposed.maxlock.util.*
import java.util.*

class SettingsActivity : AppCompatActivity(), AuthenticationSucceededListener, PreferencesAdapter.OnScreenChangeListener {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var appListViewModel: AppListModel
    private val originalTitle by lazy { applicationName.toString() }
    private val preferencesAdapter get() = settingsViewModel.preferencesAdapter
    private val viewRoot by lazy { findViewById<ViewGroup>(R.id.content_view_settings) }
    private lateinit var masterSwitch: Switch
    private val uiComponents by lazy { findViewById<Group>(R.id.ui_components) }
    private val recyclerView by lazy { findViewById<RecyclerView>(android.R.id.list) }
    private val progress by lazy { findViewById<ProgressBar>(android.R.id.progress) }
    private val lockscreen by lazy { findViewById<LockView>(R.id.lockscreen) }
    private var ctConnection: CustomTabsServiceConnection? = null
    private var ctSession: CustomTabsSession? = null
    private val devicePolicyManager by lazy { getSystemService<DevicePolicyManager>() }
    private val deviceAdmin by lazy { ComponentName(this, SettingsActivity.UninstallProtectionReceiver::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        Util.setTheme(this)
        super.onCreate(savedInstanceState)
        // Setup UI and Toolbar
        setContentView(R.layout.activity_new_settings)
        setSupportActionBar(findViewById(R.id.toolbar))

        // ViewModels
        settingsViewModel = ViewModelProviders.of(this).get()
        appListViewModel = ViewModelProviders.of(this).get()

        // Setup Preferences
        if (recyclerView.adapter == null) {
            recyclerView.adapter = preferencesAdapter.apply {
                onScreenChangeListener = this@SettingsActivity
                restoreAndObserveScrollPosition(recyclerView)
            }
        }

        // Restore state if possible/needed
        savedInstanceState?.apply {
            when (getInt(ADAPTER_SAVED_STATE)) {
                ADAPTER_APP_LIST -> {
                    openAppList()
                }
            }
            // Restore preference adapter state from saved state, if needed
            getParcelable<PreferencesAdapter.SavedState>(PREF_ADAPTER_SAVED_STATE)
                    ?.let(preferencesAdapter::loadSavedState)
        }

        // Setup event listeners
        observePreferenceClicks()
        appListViewModel.appsLoadedListener.observe(this, Observer {
            progress.isVisible = false
        })

        // Initialize custom tabs service
        bindCustomTabsService()
    }

    override fun onStart() {
        super.onStart()
        if (devicePolicyManager?.isAdminActive(deviceAdmin) == true) {
            settingsViewModel.prefUninstall.apply {
                titleRes = R.string.pref_uninstall
                summaryRes = -1
                requestRebind()
            }
        }
        if (settingsViewModel.refreshPrefDonate())
            settingsViewModel.prefDonate.requestRebind()

        // Show lockscreen if needed
        if (settingsViewModel.locked && !prefs.getString(Common.LOCKING_TYPE, "").isNullOrEmpty()) {
            uiComponents.isVisible = false
            setupWindow(true)
            lockscreen.isVisible = true
        }
    }

    private fun setupWindow(showWallpaper: Boolean) {
        window.setFlags(if (showWallpaper) FLAG_SHOW_WALLPAPER else 0, FLAG_SHOW_WALLPAPER)
        window.statusBarColor = if (showWallpaper) Color.TRANSPARENT else getColorCompat(R.color.primary_red_dark)
        if (showWallpaper) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                window.decorView.apply {
                    systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
            }
            applyCustomBackground()
            window.navigationBarColor = Color.TRANSPARENT
        } else {
            withAttrs(android.R.attr.windowBackground, android.R.attr.navigationBarColor) {
                val res = getResourceId(0, R.color.windowBackground)
                window.setBackgroundDrawableResource(res)
                window.navigationBarColor = getColor(1, Color.BLACK)
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                withAttrs(android.R.attr.windowLightNavigationBar) {
                    if (getBoolean(0, false)) {
                        window.decorView.apply {
                            systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        masterSwitch = (menu.findItem(R.id.toolbar_master_switch).actionView as Switch).apply {
            isChecked = prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)
            setOnCheckedChangeListener { _, b ->
                prefsApps.edit().putBoolean(Common.MASTER_SWITCH_ON, b).apply()
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        masterSwitch.isChecked = prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_info -> {
                val intent = CustomTabsIntent.Builder(ctSession)
                        .setShowTitle(true)
                        .enableUrlBarHiding()
                        .setToolbarColor(ContextCompat.getColor(this, R.color.primary_red))
                        .build()
                intent.launchUrl(this, Common.WEBSITE_URI)
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return false
            }
            else -> return false
        }
    }

    override fun onAuthenticationSucceeded() {
        TransitionManager.beginDelayedTransition(window.decorView as ViewGroup, Fade())
        setupWindow(false)
        uiComponents.isVisible = true
        lockscreen.isVisible = false
        settingsViewModel.locked = false
    }

    override fun onScreenChanged(screen: PreferenceScreen, subScreen: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(subScreen)
        title = when {
            screen.title.startsWith(getString(R.string.app_name)) -> getString(R.string.pref_screen_about)
            screen.titleRes != -1 -> getString(screen.titleRes)
            screen.title.isNotEmpty() -> screen.title
            else -> originalTitle
        }
        recyclerView.scrollToPosition(0)
    }

    private fun openAppList() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.pref_screen_apps)
        recyclerView.adapter = appListViewModel.adapter
    }

    private fun observePreferenceClicks() {
        settingsViewModel.activityPreferenceClickListener.observe(this, Observer { preferenceKey ->
            when (preferenceKey) {
                CHOOSE_APPS -> {
                    openAppList()
                    if (appListViewModel.loadIfNeeded())
                        progress.isVisible = true
                }
                USE_DARK_STYLE, USE_AMOLED_BLACK -> recreate()
                UNINSTALL -> {
                    if (devicePolicyManager?.isAdminActive(deviceAdmin) != true) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin)
                        startActivity(intent)
                    } else {
                        devicePolicyManager?.removeActiveAdmin(deviceAdmin)
                        settingsViewModel.prefUninstall.apply {
                            titleRes = R.string.pref_prevent_uninstall
                            summaryRes = R.string.pref_prevent_uninstall_summary
                            requestRebind()
                        }
                        val uninstall = Intent(Intent.ACTION_DELETE)
                        uninstall.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        uninstall.data = Uri.parse("package:de.Maxr1998.xposed.maxlock")
                        startActivity(uninstall)
                    }
                }
            }
        })
    }

    override fun onBackPressed() {
        if (recyclerView.adapter != preferencesAdapter) {
            recyclerView.apply {
                progress.isVisible = false
                onScreenChanged(preferencesAdapter.currentScreen, preferencesAdapter.isInSubScreen())
                adapter = preferencesAdapter
            }
        } else if (!preferencesAdapter.goBack())
            super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ADAPTER_SAVED_STATE, when (recyclerView.adapter?.javaClass) {
            AppListAdapter::class.java -> ADAPTER_APP_LIST
            else -> 0
        })
        outState.putParcelable(PREF_ADAPTER_SAVED_STATE, preferencesAdapter.getSavedState())
    }

    override fun onDestroy() {
        preferencesAdapter.onScreenChangeListener = null
        recyclerView.adapter = null
        ctConnection?.let(this::unbindService)
        super.onDestroy()
    }

    // Chrome custom tabs
    private fun bindCustomTabsService() {
        val ctPackageName = CustomTabsClient.getPackageName(this, Arrays.asList(
                "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "org.mozilla.firefox", "org.mozilla.firefox_beta"
        )) ?: return

        ctConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(componentName: ComponentName, customTabsClient: CustomTabsClient) {
                customTabsClient.warmup(0)
                customTabsClient.newSession(CustomTabsCallback())?.let {
                    ctSession = it
                    val maxr1998Website = Bundle()
                    maxr1998Website.putParcelable(CustomTabsService.KEY_URL, Common.MAXR1998_URI)
                    val technoSparksProfile = Bundle()
                    technoSparksProfile.putParcelable(CustomTabsService.KEY_URL, Common.TECHNO_SPARKS_URI)
                    it.mayLaunchUrl(Common.WEBSITE_URI, null, Arrays.asList(maxr1998Website, technoSparksProfile))
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
        CustomTabsClient.bindCustomTabsService(this, ctPackageName, ctConnection)
    }

    companion object {
        const val ADAPTER_SAVED_STATE = "adapter_state"
        const val PREF_ADAPTER_SAVED_STATE = "pref_adapter_state"
        const val ADAPTER_APP_LIST = 1
    }
}