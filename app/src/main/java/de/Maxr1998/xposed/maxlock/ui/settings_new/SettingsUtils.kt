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