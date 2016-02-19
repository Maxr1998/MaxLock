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
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.util.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static de.Maxr1998.xposed.maxlock.util.MLPreferences.getPreferencesKeys;
import static de.Maxr1998.xposed.maxlock.util.MLPreferences.getPreferencesKeysPerApp;

@SuppressLint("ViewConstructor")
public final class LockView extends RelativeLayout implements View.OnClickListener, View.OnLongClickListener {

    private final String mPackageName, mActivityName;
    private final String mLockingType, mPassword;
    private final Point screenSize = new Point();
    private final AuthenticationSucceededListener authenticationSucceededListener;
    private final FrameLayout mContainer;
    private final ViewGroup mInputBar;
    private int statusBarHeight, navBarHeight;
    private StringBuilder mCurrentKey = new StringBuilder(10);
    private TextView mInputTextView;
    private KnockCodeHelper mKnockCodeHolder;

    public LockView(Context context, String packageName, String activityName) {
        super(context);
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) getContext();
        } catch (ClassCastException e) {
            throw new RuntimeException(getContext().getClass().getSimpleName() + "must implement AuthenticationSucceededListener to use this fragment", e);
        }
        mPackageName = packageName;
        mActivityName = activityName;

        mLockingType = getPreferencesKeysPerApp(getContext()).getString(mPackageName, getPrefs().getString(Common.LOCKING_TYPE, ""));

        if (getPreferencesKeysPerApp(getContext()).contains(mPackageName)) {
            mPassword = getPreferencesKeysPerApp(getContext()).getString(mPackageName + Common.APP_KEY_PREFERENCE, null);
        } else
            mPassword = getPreferencesKeys(getContext()).getString(Common.KEY_PREFERENCE, "");

        // Dimensions
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getSize(screenSize);
        try {
            statusBarHeight = getDimens(getResources().getIdentifier("status_bar_height", "dimen", "android"));
            navBarHeight = getDimens(getResources().getIdentifier(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android"));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            statusBarHeight = 0;
            navBarHeight = 0;
        }
        LayoutInflater.from(getThemedContext()).inflate(R.layout.fragment_lock, this, true);

        TextView mTitleTextView = (TextView) findViewById(R.id.title_view);
        mInputBar = (ViewGroup) findViewById(R.id.input_bar);
        mInputTextView = (TextView) findViewById(R.id.input_view);
        ImageButton mDeleteButton = (ImageButton) findViewById(R.id.delete_input);
        mContainer = (FrameLayout) findViewById(R.id.container);

        // Background
        Util.getBackground((ImageView) findViewById(R.id.background));

        // Gaps for Status and Nav bar
        if (!getContext().getClass().getName().equals(SettingsActivity.class.getName())) {
            View gapTop = findViewById(R.id.status_bar_gap);
            View gapBottom = findViewById(R.id.nav_bar_gap);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Portrait
                gapBottom.getLayoutParams().height = navBarHeight;
                screenSize.y = screenSize.y + navBarHeight;
            } else {
                // Landscape
                //noinspection SuspiciousNameCombination
                gapBottom.getLayoutParams().width = navBarHeight;
            }
            gapTop.getLayoutParams().height = statusBarHeight;
        }

        // Locking type view setup
        switch (mLockingType) {
            case Common.PREF_VALUE_PASSWORD:
            case Common.PREF_VALUE_PASS_PIN:
                mInputBar.removeAllViews();
                mInputTextView = new AppCompatEditText(mInputBar.getContext());
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
                                Util.hideKeyboardFromWindow((Activity) getContext(), LockView.this);
                            } else {
                                v.setText("");
                                mCurrentKey.setLength(0);
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
                                Util.hideKeyboardFromWindow((Activity) getContext(), LockView.this);
                            }
                        }
                    }
                });
                mInputBar.addView(mInputTextView);
                int dp16 = Util.dpToPx(getContext(), 16);
                ((LinearLayout.LayoutParams) mInputBar.getLayoutParams()).setMargins(dp16, 0, dp16, 0);
                removeView(findViewById(R.id.fingerprint_stub));
                FrameLayout fingerprintStub = new FrameLayout(getThemedContext());
                fingerprintStub.setId(R.id.fingerprint_stub);
                mInputBar.addView(fingerprintStub);
                ((Activity) getContext()).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                break;
            case Common.PREF_VALUE_PIN:
                mContainer.addView(new PinView(getThemedContext(), this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                break;
            case Common.PREF_VALUE_KNOCK_CODE:
                mContainer.setOnLongClickListener(this);
                mKnockCodeHolder = new KnockCodeHelper(this, mContainer);
                break;
            case Common.PREF_VALUE_PATTERN:
                mInputBar.setVisibility(View.GONE);
                mContainer.addView(new PatternView(getThemedContext(), this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                break;
            default:
                authenticationSucceededListener.onAuthenticationSucceeded();
                return;
        }

        // Title
        if (getPrefs().getBoolean(Common.HIDE_TITLE_BAR, false)) {
            mTitleTextView.setVisibility(View.GONE);
        } else {
            mTitleTextView.setText(Util.getApplicationNameFromPackage(mPackageName, getContext()));
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

        if (!mLockingType.equals(Common.PREF_VALUE_KNOCK_CODE) && isTablet()) {
            // Header views
            ((ViewGroup.MarginLayoutParams) mTitleTextView.getLayoutParams()).setMargins(getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_bottom), getDimens(R.dimen.tablet_margin_sides), 0);
            ((ViewGroup.MarginLayoutParams) mInputBar.getLayoutParams()).setMargins(getDimens(R.dimen.tablet_margin_sides), 0, getDimens(R.dimen.tablet_margin_sides), 0);
            // Container
            ((ViewGroup.MarginLayoutParams) mContainer.getLayoutParams()).setMargins(getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_top), getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_bottom));
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !getPrefs().getBoolean(Common.DISABLE_FINGERPRINT, false)) {
            FingerprintView fv = new FingerprintView(getThemedContext(), (AuthenticationSucceededListener) getContext());
            fv.setScaleType(ImageView.ScaleType.CENTER);
            ((FrameLayout) findViewById(R.id.fingerprint_stub)).addView(fv);
        }

        if (getPrefs().getBoolean(Common.INVERT_COLOR, false)) {
            mTitleTextView.setTextColor(Color.BLACK);
            mInputTextView.setTextColor(Color.BLACK);
            mDeleteButton.setColorFilter(android.R.color.black, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public SharedPreferences getPrefs() {
        return MLPreferences.getPreferences(getContext());
    }

    public void appendToInput(String value) {
        mInputTextView.append(value);
    }

    public void setKey(@Nullable String value, boolean append) {
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
        }
    }

    public boolean checkInput() {
        if (Util.shaHash(mCurrentKey.toString()).equals(mPassword) || mPassword.equals("")) {
            authenticationSucceededListener.onAuthenticationSucceeded();
            return true;
        }
        return false;
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

    // Helpers
    private Context getThemedContext() {
        return new ContextThemeWrapper(getContext(), R.style.AppTheme_Dark);
    }

    public int getDimens(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }

    private boolean isTablet() {
        return getPrefs().getBoolean(Common.OVERRIDE_TABLET_MODE, getResources().getBoolean(R.bool.tablet_mode_default));
    }

    public boolean isLandscape() {
        return screenSize.x > screenSize.y;
    }
}