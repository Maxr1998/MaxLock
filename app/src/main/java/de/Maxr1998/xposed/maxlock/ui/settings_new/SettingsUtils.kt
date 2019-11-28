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

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.content.edit
import de.Maxr1998.xposed.maxlock.BuildConfig
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.Common.PREF_VALUE_PASSWORD
import de.Maxr1998.xposed.maxlock.Common.PREF_VALUE_PASS_PIN
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.*
import de.Maxr1998.xposed.maxlock.util.Util.shaHash
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

const val BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE = 100

@SuppressLint("InflateParams")
fun SettingsActivity.setupPassword(app: String?) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_password, null)
    val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setCancelable(false)
            .create().apply { show() }
    val passwordView = dialogView.findViewById<EditText>(R.id.dialog_input_password).apply {
        requestFocus()
        showIme()
    }
    val passwordConfirmView = dialogView.findViewById<EditText>(R.id.dialog_input_password_repeat)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val password = passwordView.text.toString()
        val passwordConfirm = passwordConfirmView.text.toString()
        when {
            password.isEmpty() -> {
                Toast.makeText(this, R.string.toast_password_null, Toast.LENGTH_SHORT).show()
                passwordConfirmView.setText("")
                passwordView.requestFocus()
            }
            password != passwordConfirm -> {
                Toast.makeText(this, R.string.toast_password_inconsistent, Toast.LENGTH_SHORT).show()
                passwordView.setText("")
                passwordConfirmView.setText("")
                passwordView.requestFocus()
            }
            else -> {
                dialogView.findFocus()?.clearFocus()
                dialog.dismiss()
                hideIme()
                if (app == null) {
                    prefs.edit {
                        putString(Common.LOCKING_TYPE, (
                                if (password.matches("[0-9]+".toRegex())) PREF_VALUE_PASS_PIN else PREF_VALUE_PASSWORD)
                        )
                    }
                    prefsKey.edit {
                        putString(Common.KEY_PREFERENCE, shaHash(password))
                    }
                } else {
                    prefsKeysPerApp.edit {
                        putString(app, if (password.matches("[0-9]+".toRegex())) PREF_VALUE_PASS_PIN else PREF_VALUE_PASSWORD)
                        putString(app + Common.APP_KEY_PREFERENCE, shaHash(password))
                    }
                }
                Toast.makeText(this, R.string.toast_password_changed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
        dialogView.findFocus()?.clearFocus()
        dialog.dismiss()
        hideIme()
    }
}

fun SettingsActivity.checkStoragePermission(code: Int, @StringRes dialogMessage: Int) {
    if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
        AlertDialog.Builder(this)
                .setMessage(dialogMessage)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), code)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create().show()
    } else onRequestPermissionsResult(code, arrayOf(WRITE_EXTERNAL_STORAGE), intArrayOf(PackageManager.PERMISSION_GRANTED))
}

fun deviceInfo() = """App Version: ${BuildConfig.VERSION_NAME}

Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})
API: $SDK_INT
Fingerprint: ${Build.FINGERPRINT}"""

fun SettingsActivity.prepareSendFeedback() {
    val outputCache = cacheDir("feedback-cache")
    try {
        // Obtain data
        File(Util.dataDir(this), "shared_prefs").copyRecursively(outputCache("settings"))
        File(outputCache, "device-info.txt").writeText(deviceInfo(), Charset.forName("UTF-8"))
        Runtime.getRuntime().exec("logcat -d").inputStream.copyTo(outputCache("logcat.txt").outputStream())

        // Zip up all files
        val exportDir = cacheDir("export").apply { mkdir() }
        val zipFile = exportDir("report.zip").apply { delete() }
        val stream = ZipOutputStream(BufferedOutputStream(zipFile.outputStream()))
        for (file in outputCache.walkTopDown()) {
            if (file.isDirectory) continue
            stream.putNextEntry(ZipEntry(file.toRelativeString(outputCache)))
            file.inputStream().copyTo(stream)
            stream.closeEntry()
        }
        stream.close()
        outputCache.deleteRecursively()
        checkStoragePermission(BUG_REPORT_STORAGE_PERMISSION_REQUEST_CODE, R.string.dialog_storage_permission_bug_report)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

// Move files and send email
fun SettingsActivity.finishSendFeedback() {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.dev_email)))
    intent.putExtra(Intent.EXTRA_SUBJECT, "MaxLock feedback on " + Build.MODEL)
    intent.putExtra(Intent.EXTRA_TEXT, "Please here describe your issue as DETAILED as possible!")
    val uri = getUriForFile(this, "de.Maxr1998.fileprovider", cacheDir("export")("report.zip"))
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    AlertDialog.Builder(this)
            .setMessage(R.string.dialog_message_bugreport_finished_select_email)
            .setPositiveButton(android.R.string.ok) { _, _ -> startActivity(Intent.createChooser(intent, getString(R.string.share_menu_title_send_email))) }
            .create().show()
}