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

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.helpers.*
import de.Maxr1998.modernpreferences.preferences.TwoStatePreference
import de.Maxr1998.xposed.maxlock.Common.*
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.settings.DonateActivity
import de.Maxr1998.xposed.maxlock.ui.settings_new.implementation.ImplementationDialogPreference
import de.Maxr1998.xposed.maxlock.util.GenericEventLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.Main
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SettingsViewModel(app: Application) : AndroidViewModel(app),
        CoroutineScope, Preference.OnClickListener, TwoStatePreference.OnCheckedChangeListener {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main
    internal var locked = true
    private val lockLifecycleCallbacks = LockLifecycleCallbacks(this)

    val activityPreferenceClickListener = GenericEventLiveData<String>()

    lateinit var prefUninstall: Preference

    init {
        launch { startup(app) }
        app.registerActivityLifecycleCallbacks(lockLifecycleCallbacks)
    }

    val preferencesAdapter = PreferencesAdapter(screen(app) {
        addPreferenceItem(ImplementationDialogPreference())
        categoryHeader(CATEGORY_LOCKING_TYPE) {
            titleRes = R.string.pref_category_locking
        }
        subScreen {
            titleRes = R.string.pref_screen_locking_type
        }
        subScreen {
            titleRes = R.string.pref_screen_locking_ui
        }
        subScreen {
            titleRes = R.string.pref_screen_locking_options
        }
        subScreen {
            titleRes = R.string.pref_screen_delayed_relock
            summaryRes = R.string.pref_screen_delayed_relock_summary
            iconRes = R.drawable.ic_access_time_24dp
        }
        categoryHeader("apps") {
            titleRes = R.string.pref_category_apps
        }
        pref(CHOOSE_APPS) {
            titleRes = R.string.pref_choose_apps
            summaryRes = R.string.pref_choose_apps_summary
            iconRes = R.drawable.ic_apps_24dp
        }
        categoryHeader(CATEGORY_APPLICATION_UI) {
            titleRes = R.string.pref_category_ui
        }
        switch(HIDE_APP_FROM_LAUNCHER) {
            titleRes = R.string.pref_hide_from_launcher
        }
        switch(USE_DARK_STYLE) {
            titleRes = R.string.pref_use_dark_style
            clickListener = this@SettingsViewModel
        }
        switch(USE_AMOLED_BLACK) {
            titleRes = R.string.pref_use_amoled_black
            summaryRes = R.string.pref_use_amoled_black_summary
            dependency = USE_DARK_STYLE
            clickListener = this@SettingsViewModel
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            switch(NEW_APP_NOTIFICATION) {
                titleRes = R.string.pref_new_app_notification
                titleRes = R.string.pref_new_app_notification_summary
            }
        categoryHeader(CATEGORY_ABOUT) {
            titleRes = R.string.pref_category_about
        }
        subScreen {
            titleRes = R.string.pref_screen_about
            summaryRes = R.string.pref_about_summary
        }
        pref(DONATE) {
            titleRes = R.string.pref_donate_upgrade_pro
            clickListener = this@SettingsViewModel
        }
        switch(ENABLE_PRO) {
            titleRes = R.string.pref_enable_pro
            summaryRes = R.string.pref_enable_pro_summary
        }
        prefUninstall = pref(UNINSTALL) {
            titleRes = R.string.pref_prevent_uninstall
            summaryRes = R.string.pref_prevent_uninstall_summary
            clickListener = this@SettingsViewModel
        }
        pref(SEND_FEEDBACK) {
            titleRes = R.string.pref_send_feedback
        }
    })

    override fun onClick(preference: Preference, holder: PreferencesAdapter.ViewHolder): Boolean {
        val context = holder.root.context
        when (preference.key) {
            // Dispatch to activity
            USE_DARK_STYLE,
            USE_AMOLED_BLACK,
            UNINSTALL -> activityPreferenceClickListener.call(preference.key)
            DONATE -> context.startActivity(Intent(context, DonateActivity::class.java))
        }
        return false
    }

    override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
        return true
    }

    override fun onCleared() {
        getApplication<Application>().unregisterActivityLifecycleCallbacks(lockLifecycleCallbacks)
    }
}