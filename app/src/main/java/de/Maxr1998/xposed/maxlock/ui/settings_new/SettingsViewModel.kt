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

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.lifecycle.AndroidViewModel
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.helpers.*
import de.Maxr1998.modernpreferences.preferences.TwoStatePreference
import de.Maxr1998.xposed.maxlock.BuildConfig
import de.Maxr1998.xposed.maxlock.Common.*
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.settings.DonateActivity
import de.Maxr1998.xposed.maxlock.ui.settings_new.implementation.ImplementationDialogPreference
import de.Maxr1998.xposed.maxlock.util.GenericEventLiveData
import de.Maxr1998.xposed.maxlock.util.application
import de.Maxr1998.xposed.maxlock.util.applicationName
import de.Maxr1998.xposed.maxlock.util.prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SettingsViewModel(app: Application) : AndroidViewModel(app),
        CoroutineScope, Preference.OnClickListener, TwoStatePreference.OnCheckedChangeListener {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main
    internal var locked = true
    private val lockLifecycleCallbacks = LockLifecycleCallbacks(this)

    val activityPreferenceClickListener = GenericEventLiveData<String>()

    lateinit var prefDonate: Preference
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
            collapseIcon = true
            categoryHeader(CATEGORY_LOCKING_TYPE) {
                titleRes = R.string.pref_screen_locking_type
            }
            pref(LOCKING_TYPE_PASSWORD) {
                titleRes = R.string.pref_locking_type_password
                clickListener = this@SettingsViewModel
            }
            pref(LOCKING_TYPE_PIN) {
                titleRes = R.string.pref_locking_type_pin
                clickListener = this@SettingsViewModel
            }
            pref(LOCKING_TYPE_KNOCK_CODE) {
                titleRes = R.string.pref_locking_type_knockcode
                clickListener = this@SettingsViewModel
            }
            pref(LOCKING_TYPE_PATTERN) {
                titleRes = R.string.pref_locking_type_pattern
                clickListener = this@SettingsViewModel
            }
            if (FingerprintManagerCompat.from(app).isHardwareDetected) {
                categoryHeader(CATEGORY_FINGERPRINT) {
                    titleRes = R.string.pref_category_fingerprint
                }
                switch(DISABLE_FINGERPRINT) {
                    titleRes = R.string.pref_disable_fingerprint
                    summaryRes = R.string.pref_disable_fingerprint_summary
                    summaryOnRes = R.string.pref_disable_fingerprint_summary_on
                }
            }
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
            clickListener = this@SettingsViewModel
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
            title = app.applicationName.append(" ").append(BuildConfig.VERSION_NAME).toString()
            summaryRes = R.string.pref_about_summary
            collapseIcon = true
            categoryHeader(CATEGORY_ABOUT) {
                titleRes = R.string.pref_screen_about
            }
            expandText("ml_license") {
                titleRes = R.string.app_name
                summaryRes = R.string.maxlock_author
                textRes = R.string.license_maxlock
            }
            pref(SHOW_CHANGELOG) {
                titleRes = R.string.pref_show_changelog
            }
            pref(VISIT_WEBSITE) {
                titleRes = R.string.pref_visit_website
            }
            categoryHeader(CATEGORY_CREDITS) {
                titleRes = R.string.pref_category_credits
            }
            pref("credits_fmm") {
                titleRes = R.string.credits_fmm
                summaryRes = R.string.credits_fmm_summary
            }
            pref(TECHNOSPARKS_PROFILE) {
                titleRes = R.string.credits_ts
                summaryRes = R.string.credits_ts_summary
            }
            pref("credits_intika") {
                titleRes = R.string.credits_intika
                summaryRes = R.string.credits_intika_summary
            }
            categoryHeader(CATEGORY_TRANSLATIONS) {
                titleRes = R.string.pref_category_translations
            }
            categoryHeader(CATEGORY_LICENSES) {
                titleRes = R.string.pref_category_licenses
            }
            expandText("iab_license") {
                titleRes = R.string.android_in_app_billing_v3_name
                summaryRes = R.string.android_in_app_billing_v3_author
                textRes = R.string.license_android_in_app_billing_v3
            }
            expandText("lp_license") {
                titleRes = R.string.android_lockpattern_name
                summaryRes = R.string.android_lockpattern_author
                textRes = R.string.license_android_lockpattern
            }
            expandText("ax_license") {
                titleRes = R.string.android_support_library_name
                summaryRes = R.string.the_android_open_source_project_author
                textRes = R.string.license_the_android_open_source_project
            }
            expandText("cio_license") {
                titleRes = R.string.apache_commons_io_name
                summaryRes = R.string.apache_commons_io_author
                textRes = R.string.license_apache_commons_io
            }
            expandText("fpi_license") {
                titleRes = R.string.fingerprint_icon_name
                summaryRes = R.string.the_android_open_source_project_author
                textRes = R.string.license_the_android_open_source_project
            }
            expandText("gps_license") {
                titleRes = R.string.google_play_services_name
                summaryRes = R.string.google_play_services_author
                textRes = R.string.license_google_play_services
            }
            expandText("kt_license") {
                titleRes = R.string.kotlin_libraries_name
                summaryRes = R.string.kotlin_libraries_author
                textRes = R.string.license_kotlin_libraries
            }
            expandText("mi_license") {
                titleRes = R.string.material_intro_name
                summaryRes = R.string.material_intro_author
                textRes = R.string.license_material_intro
            }
            expandText("map_license") {
                titleRes = R.string.modern_android_preferences_name
                summaryRes = R.string.modern_android_preferences_author
                textRes = R.string.license_modern_android_preferences
            }
            expandText("rp_license") {
                titleRes = R.string.remote_preferences_name
                summaryRes = R.string.remote_preferences_author
                textRes = R.string.license_remote_preferences
            }
            expandText("xb_license") {
                titleRes = R.string.xposed_bridge_name
                summaryRes = R.string.xposed_bridge_author
                textRes = R.string.license_xposed_bridge
            }
        }
        pref(DONATE) {
            prefDonate = this
            refreshPrefDonate()
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
            clickListener = this@SettingsViewModel
        }
    })

    override fun onClick(preference: Preference, holder: PreferencesAdapter.ViewHolder): Boolean {
        val context = holder.root.context
        when (preference.key) {
            // Dispatch to activity
            CHOOSE_APPS,
            USE_DARK_STYLE,
            USE_AMOLED_BLACK,
            UNINSTALL,
            SEND_FEEDBACK -> activityPreferenceClickListener.call(preference.key)
            DONATE -> context.startActivity(Intent(context, DonateActivity::class.java))
        }
        return false
    }

    override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
        return true
    }

    /**
     * @return if we should rebind the preference
     */
    fun refreshPrefDonate(): Boolean {
        val donated = application.prefs.getBoolean(DONATED, false)
        if (!donated && prefDonate.summaryRes == -1)
            return false
        prefDonate.titleRes = if (donated) R.string.pref_donate_thanks_for_donation else R.string.pref_donate_upgrade_pro
        prefDonate.summaryRes = if (donated) R.string.pref_donate_again_on_pro_summary else -1
        return true
    }

    override fun onCleared() {
        application.unregisterActivityLifecycleCallbacks(lockLifecycleCallbacks)
    }
}