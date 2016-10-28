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
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.actions.tasker.TaskerEventQueryReceiver;
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static de.Maxr1998.xposed.maxlock.util.MLPreferences.getPreferencesKeys;
import static de.Maxr1998.xposed.maxlock.util.MLPreferences.getPreferencesKeysPerApp;

@SuppressLint("ViewConstructor")
public final class LockView extends RelativeLayout implements View.OnClickListener, View.OnLongClickListener {

    private final int MAX_ATTEMPTS = 5;

    private final String mPackageName, mActivityName;
    private final String mLockingType, mPassword;
    private final Point screenSize = new Point();
    private final AuthenticationSucceededListener authenticationSucceededListener;
    private final FrameLayout mContainer;
    private final ViewGroup mInputBar;
    private StringBuilder mCurrentKey = new StringBuilder(10);
    private TextView mInputTextView;
    private TextView mMessageArea;
    private KnockCodeHelper mKnockCodeHolder;

    public LockView(ContextThemeWrapper context, String packageName, String activityName) {
        super(context);
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException(getActivity().getClass().getSimpleName() + "must implement AuthenticationSucceededListener to use this fragment", e);
        }

        String title = packageName.equals(Common.MASTER_SWITCH_ON) ? getResources().getString(R.string.unlock_master_switch) :
                Util.getApplicationNameFromPackage(packageName, getContext());
        mPackageName = packageName.equals(Common.MASTER_SWITCH_ON) ? BuildConfig.APPLICATION_ID : packageName;
        mActivityName = activityName;

        mLockingType = getPreferencesKeysPerApp(getContext()).getString(mPackageName, getPrefs().getString(Common.LOCKING_TYPE, ""));

        if (getPreferencesKeysPerApp(getContext()).contains(mPackageName)) {
            mPassword = getPreferencesKeysPerApp(getContext()).getString(mPackageName + Common.APP_KEY_PREFERENCE, null);
        } else {
            mPassword = getPreferencesKeys(getContext()).getString(Common.KEY_PREFERENCE, "");
        }

        // Dimensions
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);

        LayoutInflater.from(getContext()).inflate(R.layout.lock_view, this, true);

        TextView mTitleTextView = (TextView) findViewById(R.id.title_view);
        mInputBar = (ViewGroup) findViewById(R.id.input_bar);
        mInputTextView = (TextView) findViewById(R.id.input_view);
        ImageButton mDeleteButton = (ImageButton) findViewById(R.id.delete_input);
        mMessageArea = (TextView) findViewById(R.id.message_area);
        mContainer = (FrameLayout) findViewById(R.id.container);

        // Background
        Util.getBackground(getActivity(), (ImageView) findViewById(R.id.background));

        // Locking type view setup
        switch (mLockingType) {
            case Common.PREF_VALUE_PASSWORD:
            case Common.PREF_VALUE_PASS_PIN:
                mInputBar.removeAllViews();
                mInputTextView = new AppCompatEditText(getContext());
                LinearLayout.LayoutParams mInputTextParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
                mInputTextParams.weight = 1;
                mInputTextView.setLayoutParams(mInputTextParams);
                mInputTextView.setSingleLine();
                if (mLockingType.equals(Common.PREF_VALUE_PASS_PIN)) {
                    mInputTextView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                } else {
                    mInputTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                mInputTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            if (checkInput()) {
                                Util.hideKeyboardFromWindow(getActivity(), LockView.this);
                            } else {
                                setKey(null, false);
                                v.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake));
                                handleFailedAttempt();
                            }
                            return true;
                        }
                        return false;
                    }
                });
                mInputTextView.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        setKey(editable.toString(), false);
                        if (getPrefs().getBoolean(Common.ENABLE_QUICK_UNLOCK, false)) {
                            if (checkInput()) {
                                Util.hideKeyboardFromWindow(getActivity(), LockView.this);
                            }
                        }
                    }
                });
                mInputBar.addView(mInputTextView);
                int dp16 = Util.dpToPx(getContext(), 16);
                ((LinearLayout.LayoutParams) mInputBar.getLayoutParams()).setMargins(dp16, 0, dp16, 0);
                removeView(findViewById(R.id.fingerprint_stub));
                FrameLayout fingerprintStub = new FrameLayout(getContext());
                fingerprintStub.setId(R.id.fingerprint_stub);
                mInputBar.addView(fingerprintStub);
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                break;
            case Common.PREF_VALUE_PIN:
                FrameLayout.LayoutParams pinParams = new FrameLayout.LayoutParams(getDimens(R.dimen.container_size), getDimens(R.dimen.container_size));
                pinParams.gravity = isLandscape() ? Gravity.CENTER : Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                pinParams.bottomMargin = isLandscape() ? 0 : getDimens(R.dimen.fingerprint_margin);
                mContainer.addView(new PinView(getContext(), this), pinParams);
                break;
            case Common.PREF_VALUE_KNOCK_CODE:
                mKnockCodeHolder = new KnockCodeHelper(this, mContainer);
                mContainer.setOnLongClickListener(this);
                mContainer.setContentDescription(getResources().getString(R.string.content_description_lockscreen_container));
                break;
            case Common.PREF_VALUE_PATTERN:
                mInputBar.setVisibility(View.GONE);
                FrameLayout.LayoutParams patternParams = new FrameLayout.LayoutParams(getDimens(R.dimen.container_size), getDimens(R.dimen.container_size));
                patternParams.gravity = Gravity.CENTER;
                mContainer.addView(new PatternView(getContext(), this), patternParams);
                break;
            default:
                handleAuthenticationSuccess();
                return;
        }

        // Title
        if (getPrefs().getBoolean(Common.HIDE_TITLE_BAR, false)) {
            mTitleTextView.setVisibility(View.GONE);
        } else {
            mTitleTextView.setText(title);
            mTitleTextView.setCompoundDrawablesWithIntrinsicBounds(Util.getApplicationIconFromPackage(mPackageName, getContext()), null, null, null);
            mTitleTextView.setOnLongClickListener(this);
        }

        //Input
        if (!mLockingType.equals(Common.PREF_VALUE_PASSWORD) && !mLockingType.equals(Common.PREF_VALUE_PASS_PIN)) {
            if (getPrefs().getBoolean(Common.HIDE_INPUT_BAR, false)) {
                mInputBar.setVisibility(View.GONE);
            } else {
                mInputTextView.setText("");
                mDeleteButton.setOnClickListener(this);
                mDeleteButton.setOnLongClickListener(this);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !getPrefs().getBoolean(Common.DISABLE_FINGERPRINT, false)) {
            FingerprintView fv = new FingerprintView(getContext(), this);
            ((FrameLayout) findViewById(R.id.fingerprint_stub)).addView(fv);
        }

        // Handle timer for previous wrong attempts
        if (isTimeLeft()) {
            handleTimer();
        }
    }

    /**
     * Must be used as ContextThemeWrapper context for this LockView
     */
    public static ContextThemeWrapper getThemedContext(Context baseContext) {
        return new ContextThemeWrapper(baseContext, MLPreferences.getPreferences(baseContext).getBoolean(Common.INVERT_COLOR, false) ? R.style.AppTheme : R.style.AppTheme_Dark);
    }

    public SharedPreferences getPrefs() {
        return MLPreferences.getPreferences(getContext());
    }

    public void appendToInput(String value) {
        if (isTimeLeft()) {
            return;
        }
        mInputTextView.append(value);
    }

    public void setKey(@Nullable String value, boolean append) {
        if (isTimeLeft()) {
            return;
        }
        mMessageArea.setText("");
        if (value == null) {
            mCurrentKey.setLength(0);
            if (mKnockCodeHolder != null) {
                mKnockCodeHolder.clear(true);
            }
            mInputTextView.setText("");
            return;
        }
        if (!append) {
            mCurrentKey.setLength(0);
        }
        mCurrentKey.append(value);

    }

    public void setPattern(List pattern, PatternView patternView) {
        setKey(pattern.toString(), false);
        if (!checkInput()) {
            patternView.setWrong();
            handleFailedAttempt();
        }
    }

    public boolean checkInput() {
        if (!isTimeLeft() && Util.shaHash(mCurrentKey.toString()).equals(mPassword) || mPassword.equals("")) {
            handleAuthenticationSuccess();
            return true;
        }
        return false;
    }

    public boolean handleAuthenticationSuccess() {
        getPrefs().edit().putInt(Common.FAILED_ATTEMPTS_COUNTER, 0).apply();
        authenticationSucceededListener.onAuthenticationSucceeded();
        TaskerEventQueryReceiver.sendRequest(getActivity(), true, mPackageName);
        return true;
    }

    public void handleFailedAttempt() {
        if (isTimeLeft()) {
            return;
        }
        mMessageArea.setText(R.string.message_wrong_password);
        int old = getPrefs().getInt(Common.FAILED_ATTEMPTS_COUNTER, 0);
        getPrefs().edit().putInt(Common.FAILED_ATTEMPTS_COUNTER, ++old).apply();
        if (old % MAX_ATTEMPTS == 0) {
            setKey(null, false);
            getPrefs().edit().putLong(Common.FAILED_ATTEMPTS_TIMER, System.currentTimeMillis()).apply();
            handleTimer();
        }
        TaskerEventQueryReceiver.sendRequest(getActivity(), false, mPackageName);
    }

    private void handleTimer() {
        new CountDownTimer(getTimeLeft(), 200) {
            @Override
            public void onTick(long millisUntilFinished) {
                mMessageArea.setText(getResources().getString(R.string.message_try_again_in_seconds, millisUntilFinished / 1000 + 1));
            }

            @Override
            public void onFinish() {
                mMessageArea.setText("");
            }
        }.start();
    }

    private long getTimeLeft() {
        return 59000 + getPrefs().getLong(Common.FAILED_ATTEMPTS_TIMER, 0) - System.currentTimeMillis();
    }

    private boolean isTimeLeft() {
        return getTimeLeft() > 0;
    }

    public boolean allowFingerprint() {
        return !isTimeLeft() && getPrefs().getInt(Common.FAILED_ATTEMPTS_COUNTER, 0) < MAX_ATTEMPTS;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.delete_input:
                if (mCurrentKey.length() > 0) {
                    mCurrentKey.deleteCharAt(mCurrentKey.length() - 1);
                    if (mInputTextView.length() > 0) {
                        mInputTextView.setText(mInputTextView.getText().subSequence(0, mInputTextView.getText().length() - 1));
                    }
                    if (mKnockCodeHolder != null) {
                        mKnockCodeHolder.clear(false);
                    }
                }
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.title_view:
                Toast.makeText(getContext(), mActivityName, Toast.LENGTH_SHORT).show();
                return true;
            default:
                setKey(null, false);
                return true;
        }
    }

    public String getPackageName() {
        return mPackageName;
    }

    // Helpers
    public AppCompatActivity getActivity() {
        return (AppCompatActivity) ((ContextWrapper) getContext()).getBaseContext();
    }

    public int getDimens(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }

    public boolean isLandscape() {
        return screenSize.x > screenSize.y;
    }
}