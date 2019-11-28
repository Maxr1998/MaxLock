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

package de.Maxr1998.xposed.maxlock.ui.lockscreen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView.ScaleType.CENTER
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.view.isVisible
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.actions.tasker.TaskerHelper
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.Util
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("ViewConstructor")
class FingerprintView(context: Context, private val lockView: LockView) : AppCompatImageView(context) {
    private val fingerprintManager = FingerprintManagerCompat.from(context)
    private val authenticating = AtomicBoolean(false)
    private var cancellationSignal: CancellationSignal? = null
    private val authenticationCallback = object : FingerprintManagerCompat.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            if (lockView.allowFingerprint()) { // Make sure we're still allowed to authenticate
                handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_draw_off_animation)
                lockView.handleAuthenticationSuccess()
            } else cancelAuthentication()
        }

        override fun onAuthenticationFailed() {
            handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_fp_to_error_state_animation)
            if (lockView.allowFingerprint()) {
                postDelayed({ handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_error_state_to_fp_animation) }, 800)
            } else {
                cancellationSignal?.cancel()
                setOnClickListener(fpDisabledToast)
            }
            TaskerHelper.sendQueryRequest(getContext(), false, lockView.packageName)
        }
    }
    private val fpDisabledToast = OnClickListener { Toast.makeText(getContext(), R.string.message_fingerprint_disabled, Toast.LENGTH_SHORT).show() }

    init {
        scaleType = CENTER
        contentDescription = resources.getString(android.R.string.fingerprint_icon_content_description)
    }

    private fun handleFingerprintIndicator(@DrawableRes id: Int) {
        if (MLPreferences.getPreferences(context).getBoolean(Common.HIDE_FINGERPRINT_ICON, false)) {
            return
        }
        if (context != null) {
            val fp = context.getDrawable(id)
            if (fp is AnimatedVectorDrawable) {
                setImageDrawable(fp)
                fp.start()
            }
        }
    }

    private fun authenticate() {
        if (!authenticating.compareAndSet(false, true) || !lockView.isVisible)
            return
        if (!lockView.allowFingerprint()) {
            cancelAuthentication()
            return
        }
        Log.d(Util.LOG_TAG_FINGERPRINT, "Starting authentication..")
        if (fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()) {
            if (cancellationSignal == null || cancellationSignal!!.isCanceled) {
                cancellationSignal = CancellationSignal().apply {
                    setOnCancelListener { authenticating.set(false) }
                }
            }
            fingerprintManager.authenticate(null, 0, cancellationSignal, authenticationCallback, null)
            handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_draw_on_animation)
        } else visibility = View.GONE
    }

    /**
     * Cancel the current authentication, if needed.
     * This should only happen when a) the view is being dismissed (lockView invisible) or
     * the user failed authentication through another way
     */
    fun cancelAuthentication() {
        cancellationSignal?.cancel()
        if (lockView.isVisible && !lockView.allowFingerprint()) {
            handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_fp_to_error_state_animation)
            setOnClickListener(fpDisabledToast)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        authenticate()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) authenticate()
        else cancellationSignal?.cancel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancellationSignal?.cancel()
    }
}