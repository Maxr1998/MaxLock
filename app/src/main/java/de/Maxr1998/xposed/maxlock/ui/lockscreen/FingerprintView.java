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

package de.Maxr1998.xposed.maxlock.ui.lockscreen;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.widget.ImageView;

import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.util.Util;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@SuppressLint("ViewConstructor")
public final class FingerprintView extends ImageView {

    private final AuthenticationSucceededListener mAuthenticationSucceededListener;

    private final FingerprintManagerCompat.AuthenticationCallback mFPAuthenticationCallback = new FingerprintManagerCompat.AuthenticationCallback() {
        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            Util.hideKeyboardFromWindow((Activity) mAuthenticationSucceededListener, FingerprintView.this);
            handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_draw_off_animation);
            mAuthenticationSucceededListener.onAuthenticationSucceeded();
        }

        @Override
        public void onAuthenticationFailed() {
            handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_fp_to_error_state_animation);
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_error_state_to_fp_animation);
                }
            }, 800);
        }
    };
    private CancellationSignal mCancelFingerprint = new CancellationSignal();

    @TargetApi(Build.VERSION_CODES.M)
    public FingerprintView(Context context, AuthenticationSucceededListener listener) {
        super(context);
        mAuthenticationSucceededListener = listener;
        setScaleType(ImageView.ScaleType.CENTER);
        setContentDescription(getResources().getString(android.R.string.fingerprint_icon_content_description));
    }

    @TargetApi(LOLLIPOP)
    private void handleFingerprintIndicator(@DrawableRes int id) {
        if (getContext() != null) {
            Drawable fp = getContext().getDrawable(id);
            if (fp instanceof AnimatedVectorDrawable) {
                setImageDrawable(fp);
                ((AnimatedVectorDrawable) fp).start();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            FingerprintManagerCompat mFingerprintManager = FingerprintManagerCompat.from(getContext());
            if (mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints()) {
                if (mCancelFingerprint.isCanceled()) {
                    mCancelFingerprint = new CancellationSignal();
                }
                mFingerprintManager.authenticate(null, 0, mCancelFingerprint, mFPAuthenticationCallback, null);
                handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_draw_on_animation);
            } else {
                setVisibility(GONE);
            }
        } else {
            mCancelFingerprint.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCancelFingerprint.cancel();
    }
}