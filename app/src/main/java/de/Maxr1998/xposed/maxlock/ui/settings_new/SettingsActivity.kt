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

package de.Maxr1998.xposed.maxlock.ui.settings_new

import android.content.ComponentName
import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.browser.customtabs.*
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.recyclerview.widget.RecyclerView
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.lockscreen.LockView
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener
import de.Maxr1998.xposed.maxlock.util.Util
import de.Maxr1998.xposed.maxlock.util.prefs
import de.Maxr1998.xposed.maxlock.util.prefsApps
import java.util.*

class SettingsActivity : AppCompatActivity(), AuthenticationSucceededListener, PreferencesAdapter.OnScreenChangeListener {
    private lateinit var viewModel: SettingsViewModel
    private lateinit var originalTitle: String
    private val preferencesAdapter get() = viewModel.preferencesAdapter
    private val viewRoot by lazy { findViewById<ViewGroup>(R.id.content_view_settings) }
    private val recyclerView by lazy { findViewById<RecyclerView>(android.R.id.list) }
    private val lockscreen by lazy { LockView(this, null) }
    private var ctConnection: CustomTabsServiceConnection? = null
    private var ctSession: CustomTabsSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Util.setTheme(this)
        super.onCreate(savedInstanceState)
        // Setup UI and Toolbar
        setContentView(R.layout.activity_new_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        originalTitle = title.toString()

        // ViewModel
        viewModel = ViewModelProviders.of(this).get()

        // Preferences
        recyclerView.adapter = preferencesAdapter
        onScreenChanged(preferencesAdapter.currentScreen, preferencesAdapter.isInSubScreen())
        preferencesAdapter.onScreenChangeListener = this

        // Show lockscreen if needed
        if (!prefs.getString(Common.LOCKING_TYPE, "").isNullOrEmpty()) {
            viewRoot.forEach { it.isVisible = false }
            viewRoot.addView(lockscreen, MATCH_PARENT, MATCH_PARENT)
        }

        // Initialize custom tabs service
        bindCustomTabsService()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        (menu.findItem(R.id.toolbar_master_switch).actionView as SwitchCompat).apply {
            isChecked = prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)
            setOnCheckedChangeListener { _, b ->
                prefsApps.edit().putBoolean(Common.MASTER_SWITCH_ON, b).apply()
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        (menu.findItem(R.id.toolbar_master_switch).actionView as SwitchCompat).apply {
            isChecked = prefsApps.getBoolean(Common.MASTER_SWITCH_ON, true)
        }
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
        TransitionManager.beginDelayedTransition(viewRoot, Fade())
        viewRoot.forEach { it.isVisible = true }
        lockscreen.isVisible = false
    }

    override fun onScreenChanged(screen: PreferenceScreen, subScreen: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(subScreen)
        title = when {
            screen.titleRes != -1 -> getString(screen.titleRes)
            screen.title.isNotEmpty() -> screen.title
            else -> originalTitle
        }
        preferencesAdapter.restoreAndObserveScrollPosition(recyclerView)
    }

    override fun onBackPressed() {
        if (!preferencesAdapter.goBack())
            super.onBackPressed()
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
}