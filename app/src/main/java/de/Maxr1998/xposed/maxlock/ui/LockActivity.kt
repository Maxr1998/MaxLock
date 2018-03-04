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

package de.Maxr1998.xposed.maxlock.ui

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.CATEGORY_HOME
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.annotation.Keep
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.text.InputType
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.MLImplementation
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.lockscreen.LockView
import de.Maxr1998.xposed.maxlock.util.AppLockHelpers.appClosed
import de.Maxr1998.xposed.maxlock.util.AppLockHelpers.appUnlocked
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.NotificationHelper
import de.Maxr1998.xposed.maxlock.util.Util

class LockActivity : AppCompatActivity(), AuthenticationSucceededListener {

    private lateinit var prefs: SharedPreferences
    @Keep
    private lateinit var names: Array<String>
    private var unlocked = false
    private var fakeDieDialog: AlertDialog? = null
    private var reportDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = MLPreferences.getPreferences(this)
        val isFakeCrash = isFakeCrash(intent)
        val themeId = if (!isFakeCrash) {
            if (prefs.getBoolean(Common.HIDE_STATUS_BAR, false)) R.style.TranslucentStatusBar_Full else R.style.TranslucentStatusBar
        } else R.style.FakeDieDialog
        setTheme(themeId)
        if (isFakeCrash) window.setBackgroundDrawable(ColorDrawable(0))
        super.onCreate(savedInstanceState)
        setup(intent, isFakeCrash)
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        val fakeCrashNow = isFakeCrash(newIntent)
        if (isFakeCrash(intent) != fakeCrashNow) {
            recreate()
            return
        }
        setup(newIntent, fakeCrashNow)
    }

    private fun isFakeCrash(intent: Intent): Boolean {
        val lockActivityMode = intent.getStringExtra(Common.LOCK_ACTIVITY_MODE)
        return lockActivityMode == Common.MODE_FAKE_CRASH || prefs.getBoolean(Common.FC_ENABLE_FOR_ALL_APPS, false) && lockActivityMode != Common.MODE_UNLOCK
    }

    private fun setup(intent: Intent, isFakeCrash: Boolean) {
        names = intent.getStringArrayExtra(Common.INTENT_EXTRAS_NAMES) ?: arrayOf("", "")
        if (!isFakeCrash) {
            val lockView = LockView(this, null, names[0], names[1])
            setContentView(lockView, RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            ViewCompat.requestApplyInsets(lockView)
            return
        }

        val pm = packageManager
        val requestPkgInfo = try {
            pm.getApplicationInfo(names[0], 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        val requestPkgFullName = (if (requestPkgInfo != null) pm.getApplicationLabel(requestPkgInfo) else "(unknown)") as String
        fakeDieDialog = AlertDialog.Builder(this)
                .setMessage(String.format(resources.getString(R.string.dialog_text_fake_die_stopped), requestPkgFullName))
                .setNeutralButton(R.string.dialog_button_report) { _, _ ->
                    if (prefs.getBoolean(Common.FC_ENABLE_DIRECT_UNLOCK, false)) {
                        fakeDieDialog!!.dismiss()
                        fakeDieDialog = null
                        launchLockView()
                        finish()
                    } else {
                        fakeDieDialog!!.dismiss()
                        fakeDieDialog = null
                        val input = EditText(this@LockActivity)
                        input.minLines = 2
                        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        reportDialog = AlertDialog.Builder(this@LockActivity)
                                .setView(input)
                                .setTitle(R.string.dialog_title_report_problem)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    if (input.text.toString() == prefs.getString(Common.FC_INPUT, "start")) {
                                        reportDialog!!.dismiss()
                                        reportDialog = null
                                        if (prefs.getBoolean(Common.FC_ENABLE_PASSPHRASE_UNLOCK, false)) {
                                            onAuthenticationSucceeded()
                                        } else {
                                            launchLockView()
                                            finish()
                                        }
                                    } else {
                                        Toast.makeText(this@LockActivity, "Thanks for your feedback", Toast.LENGTH_SHORT).show()
                                        onBackPressed()
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel) { _, _ -> onBackPressed() }
                                .setOnCancelListener { onBackPressed() }
                                .create().apply { show() }
                    }
                }
                .setPositiveButton(android.R.string.ok) { _, _ -> onBackPressed() }
                .setOnCancelListener { onBackPressed() }
                .create().apply { show() }
    }

    private fun launchLockView() {
        val it = Intent(this, LockActivity::class.java)
        it.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        it.putExtra(Common.LOCK_ACTIVITY_MODE, Common.MODE_UNLOCK)
        it.putExtra(Common.INTENT_EXTRAS_NAMES, names)
        finish()
        startActivity(it)
    }

    override fun onAuthenticationSucceeded() {
        appUnlocked(names[0], MLPreferences.getPrefsHistory(this))
        unlocked = true
        NotificationHelper.postIModNotification(this)
        finish()
        overridePendingTransition(0, R.anim.lockscreen_fade_out_fast)
    }

    override fun onBackPressed() {
        Log.d(Util.LOG_TAG_LOCKSCREEN, "Pressed back.")
        if (MLImplementation.getImplementation(prefs) == MLImplementation.DEFAULT) {
            appClosed(names[0], MLPreferences.getPrefsHistory(this))
        } else {
            startActivity(Intent(ACTION_MAIN).addCategory(CATEGORY_HOME))
        }
        super.onBackPressed()
    }

    public override fun onPause() {
        fakeDieDialog?.dismiss()
        reportDialog?.dismiss()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        fakeDieDialog?.show()
        reportDialog?.show()
    }

    override fun onStop() {
        if (prefs.getBoolean(Common.ENABLE_LOGGING, false) && !unlocked) {
            Util.logFailedAuthentication(this, names[0])
        }
        super.onStop()
    }

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}