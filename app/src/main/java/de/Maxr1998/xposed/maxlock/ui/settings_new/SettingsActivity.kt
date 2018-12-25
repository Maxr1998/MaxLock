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
import android.content.pm.PackageManager.PERMISSION_GRANTED
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
import androidx.activity.viewModels
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.haibison.android.lockpattern.LockPatternActivity
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.Common.*
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity
import de.Maxr1998.xposed.maxlock.ui.lockscreen.LockView
import de.Maxr1998.xposed.maxlock.ui.settings.applist.AppListAdapter
import de.Maxr1998.xposed.maxlock.ui.settings_new.screens.AppListScreen
import de.Maxr1998.xposed.maxlock.util.*
import java.util.*

class SettingsActivity : AppCompatActivity(), AuthenticationSucceededListener, PreferencesAdapter.OnScreenChangeListener {
    private val settingsViewModel by viewModels<SettingsViewModel>()
    private val originalTitle by lazy { applicationName.toString() }
    private val preferencesAdapter get() = settingsViewModel.preferencesAdapter
    private val viewRoot by lazyView<ViewGroup>(R.id.content_view_settings)
    private var masterSwitch: Switch? = null
    private val uiComponents by lazyView<Group>(R.id.ui_components)
    private val recyclerView by lazyView<RecyclerView>(android.R.id.list)
    private val progress by lazyView<ProgressBar>(android.R.id.progress)
    private val lockscreen by lazyView<LockView>(R.id.lockscreen)
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

        // Setup Preferences
        if (recyclerView.adapter == null) {
            recyclerView.adapter = preferencesAdapter.apply {
                onScreenChangeListener = this@SettingsActivity
                restoreAndObserveScrollPosition(recyclerView)
            }
        }
        // Applies the custom screen if needed
        applyCurrentCustomScreen()

        // Restore state if possible/needed
        savedInstanceState?.apply {
            // Restore preference adapter state from saved state, if needed
            getParcelable<PreferencesAdapter.SavedState>(PREFS_SAVED_STATE)
                    ?.let(preferencesAdapter::loadSavedState)
            // Restore screen
            if (settingsViewModel.customScreenStack.isEmpty()) {
                // TODO: Restore whole stack
                when (val screen = getInt(SCREEN_SAVED_STATE)) {
                    SCREEN_DEFAULT -> return@apply
                    else -> openCustomScreen(screen)
                }
            }
        }

        // Setup event listeners
        observePreferenceClicks()

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
        masterSwitch = (menu.findItem(R.id.toolbar_master_switch)?.actionView as? Switch)?.apply {
            isChecked = prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)
            setOnCheckedChangeListener { _, b ->
                prefsApps.edit().putBoolean(Common.MASTER_SWITCH_ON, b).apply()
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        masterSwitch?.isChecked = prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)
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

    private fun openCustomScreen(@CustomScreen id: Int) {
        val screen = settingsViewModel.customScreens.get(id) ?: when (id) {
            SCREEN_APP_LIST -> AppListScreen(this)
            else -> throw RuntimeException("The specified screen id doesn't exist")
        }.also { settingsViewModel.customScreens.put(id, it) }
        settingsViewModel.customScreenStack.push(screen)
        applyCurrentCustomScreen()
    }

    private fun applyCurrentCustomScreen() {
        if (settingsViewModel.customScreenStack.isEmpty())
            return
        val screen = settingsViewModel.customScreenStack.last()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(screen.titleRes)
        if (screen.hasOptionsMenu)
            invalidateOptionsMenu()
        screen.adapter?.let {
            recyclerView.adapter = it
        }
        screen.view?.let {
            viewRoot.addView(it, ViewGroup.LayoutParams.MATCH_PARENT, 0)
            it.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = R.id.toolbar
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        screen.progressLiveData?.apply {
            progress.isVisible = true
            observe(this@SettingsActivity, Observer {
                progress.isVisible = false
            })
        }
    }

    private fun observePreferenceClicks() {
        settingsViewModel.activityPreferenceClickListener.observe(this, Observer { preferenceKey ->
            when (preferenceKey) {
                LOCKING_TYPE_PASSWORD -> setupPassword(null)
                LOCKING_TYPE_PATTERN -> {
                    val intent = Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, this, LockPatternActivity::class.java)
                    startActivityForResult(intent, KUtil.getPatternCode(-1))
                }
                CHOOSE_APPS -> openCustomScreen(SCREEN_APP_LIST)
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
                SEND_FEEDBACK -> prepareSendFeedback()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED)
                    finishSendFeedback()
            }
        }
    }

    override fun onBackPressed() {
        val stack = settingsViewModel.customScreenStack
        if (stack.isNotEmpty()) {
            progress.isVisible = false
            stack.pop()
            if (stack.isNotEmpty())
                applyCurrentCustomScreen()
            else recyclerView.apply {
                onScreenChanged(preferencesAdapter.currentScreen, preferencesAdapter.isInSubScreen())
                invalidateOptionsMenu()
                adapter = preferencesAdapter
            }
        } else if (!preferencesAdapter.goBack())
            super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SCREEN_SAVED_STATE, when (recyclerView.adapter?.javaClass) {
            AppListAdapter::class.java -> SCREEN_APP_LIST
            else -> SCREEN_DEFAULT
        })
        outState.putParcelable(PREFS_SAVED_STATE, preferencesAdapter.getSavedState())
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
        const val SCREEN_SAVED_STATE = "screen_state"
        const val PREFS_SAVED_STATE = "prefs_state"
        const val SCREEN_DEFAULT = 0
        const val SCREEN_APP_LIST = 1
    }

    @IntDef(SCREEN_DEFAULT, SCREEN_APP_LIST)
    @Retention(AnnotationRetention.SOURCE)
    annotation class CustomScreen
}