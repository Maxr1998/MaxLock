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

import android.app.Activity
import android.app.AlertDialog
import android.webkit.WebView
import android.webkit.WebViewClient
import de.Maxr1998.xposed.maxlock.BuildConfig
import de.Maxr1998.xposed.maxlock.R

object SettingsUtils {
    @JvmStatic
    fun showUpdatedMessage(a: Activity) {
        AlertDialog.Builder(a)
                .setMessage(R.string.dialog_maxlock_updated)
                .setNegativeButton(R.string.dialog_button_whats_new) { _, _ -> showChangelog(a) }
                .setPositiveButton(R.string.dialog_button_got_it, null)
                .create().apply {
                    setCanceledOnTouchOutside(false)
                }.show()
    }

    @JvmStatic
    fun showChangelog(a: Activity) {
        val wv = WebView(a).apply {
            webViewClient = WebViewClient()
            settings.userAgentString = "MaxLock App v" + BuildConfig.VERSION_NAME
            loadUrl("http://maxlock.maxr1998.de/files/changelog-base.php")
        }
        AlertDialog.Builder(a)
                .setView(wv)
                .setPositiveButton(android.R.string.ok, null)
                .create().show()
    }
}