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
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.actions.tasker.TaskerEventQueryReceiver;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@SuppressLint("ViewConstructor")
public final class FingerprintView extends ImageView {

    private final LockView mLockView;
    private final OnClickListener mNotAllowedToast = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getContext(), R.string.message_fingerprint_disabled, Toast.LENGTH_SHORT).show();
        }
    };
    private CancellationSignal mCancelFingerprint = new CancellationSignal();
    private final FingerprintManagerCompat.AuthenticationCallback mFPAuthenticationCallback = new FingerprintManagerCompat.AuthenticationCallback() {
        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            Util.hideKeyboardFromWindow(mLockView.getActivity(), FingerprintView.this);
            if (mLockView.allowFingerprint()) {
                handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_draw_off_animation);
                mLockView.handleAuthenticationSuccess();
            } else {
                onWindowFocusChanged(true);
            }
        }

        @Override
        public void onAuthenticationFailed() {
            handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_fp_to_error_state_animation);
            if (mLockView.allowFingerprint()) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_error_state_to_fp_animation);
                    }
                }, 800);
            } else {
                mCancelFingerprint.cancel();
                setOnClickListener(mNotAllowedToast);
            }
            TaskerEventQueryReceiver.sendRequest(getContext(), false, mLockView.getPackageName());
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    public FingerprintView(Context context, LockView lv) {
        super(context);
        mLockView = lv;
        setScaleType(ImageView.ScaleType.CENTER);
        setContentDescription(getResources().getString(android.R.string.fingerprint_icon_content_description));
    }

    @TargetApi(LOLLIPOP)
    private void handleFingerprintIndicator(@DrawableRes int id) {
        if (MLPreferences.getPreferences(getContext()).getBoolean(Common.HIDE_FINGERPRINT_ICON, false)) {
            return;
        }
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
                if (!mLockView.allowFingerprint()) {
                    handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_fp_to_error_state_animation);
                    setOnClickListener(mNotAllowedToast);
                    return;
                }
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