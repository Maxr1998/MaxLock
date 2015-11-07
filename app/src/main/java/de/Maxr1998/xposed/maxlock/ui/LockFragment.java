/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2015  Maxr1998
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

package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.DimenRes;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.haibison.android.lockpattern.widget.LockPatternView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.Util;

public class LockFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    private final ArrayList<Float> knockCodeX = new ArrayList<>();
    private final ArrayList<Float> knockCodeY = new ArrayList<>();
    private final Paint kCTouchColor = new Paint();
    private final List<Integer> pinButtonIds = Arrays.asList(
            R.id.pin1, R.id.pin2, R.id.pin3,
            R.id.pin4, R.id.pin5, R.id.pin6,
            R.id.pin7, R.id.pin8, R.id.pin9,
            R.id.pin0, R.id.pin_ok
    );
    private AuthenticationSucceededListener authenticationSucceededListener;
    private SharedPreferences prefs;
    private LockPatternView lockPatternView;
    private LockPatternView.OnPatternListener patternListener;
    private final Runnable mLockPatternViewReloader = new Runnable() {
        @Override
        public void run() {
            lockPatternView.clearPattern();
            patternListener.onPatternCleared();
        }
    };
    private ViewGroup rootView;
    private TextView mInputTextView;
    private FrameLayout container;
    private String requestPkg;
    private int screenHeight, screenWidth, statusBarHeight, navBarHeight;
    private String password, lockingType;
    private StringBuilder key;
    private Bitmap kCCBackground;
    private RippleDrawable kCTouchLP;
    private int containerX, containerY;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(getActivity().getClass().getSimpleName() + "must implement AuthenticationSucceededListener to use this fragment", e);
        }
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences prefsKey = getActivity().getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        SharedPreferences prefsPerApp = getActivity().getSharedPreferences(Common.PREFS_KEYS_PER_APP, Context.MODE_PRIVATE);

        // Strings
        requestPkg = getArguments().getString(Common.INTENT_EXTRAS_PKG_NAME);

        if (prefsPerApp.contains(requestPkg))
            password = prefsPerApp.getString(requestPkg + Common.APP_KEY_PREFERENCE, null);
        else password = prefsKey.getString(Common.KEY_PREFERENCE, "");

        lockingType = prefsPerApp.getString(requestPkg, prefs.getString(Common.LOCKING_TYPE, ""));


        // Constants
        key = new StringBuilder(password.length() + new Random().nextInt(32));

        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        try {
            statusBarHeight = getDimens(getResources().getIdentifier("status_bar_height", "dimen", "android"));
            navBarHeight = getDimens(getResources().getIdentifier(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android"));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            statusBarHeight = 0;
            navBarHeight = 0;
        }
        //noinspection deprecation
        kCTouchColor.setColor(getResources().getColor(R.color.legacy_highlight_dark));
        kCTouchColor.setStrokeWidth(1);
        kCTouchColor.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup mainContainer, Bundle savedInstanceState) {
        // Views
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_lock, mainContainer, false);
        ImageView background = (ImageView) rootView.findViewById(R.id.background);
        TextView mTitleTextView = (TextView) rootView.findViewById(R.id.title_view);
        View mInputBar = rootView.findViewById(R.id.input_bar);
        mInputTextView = (TextView) rootView.findViewById(R.id.input_view);
        container = (FrameLayout) rootView.findViewById(R.id.container);
        ImageButton mDeleteButton = (ImageButton) rootView.findViewById(R.id.delete_input);

        // Locking type view setup
        switch (lockingType) {
            case Common.PREF_VALUE_PASSWORD:
            case Common.PREF_VALUE_PASS_PIN:
                mTitleTextView.setVisibility(View.GONE);
                mInputBar.setVisibility(View.GONE);
                Util.askPassword(getActivity(), password, lockingType.equals(Common.PREF_VALUE_PASS_PIN));
                break;
            case Common.PREF_VALUE_PIN:
                inflater.inflate(R.layout.pin_field, container);
                setupPINLayout();
                break;
            case Common.PREF_VALUE_KNOCK_CODE:
                setupKnockCodeLayout();
                break;
            case Common.PREF_VALUE_PATTERN:
                mInputBar.setVisibility(View.GONE);
                inflater.inflate(R.layout.pattern_field, container);
                setupPatternLayout();
                break;
            default:
                authenticationSucceededListener.onAuthenticationSucceeded();
                return rootView;
        }

        // Background
        background.setImageDrawable(Util.getBackground(getActivity(), screenWidth, screenHeight));

        // Gaps for Status and Nav bar
        if (!getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.ui.SettingsActivity")) {
            View gapTop = rootView.findViewById(R.id.status_bar_gap);
            View gapBottom = rootView.findViewById(R.id.nav_bar_gap);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Portrait
                gapBottom.getLayoutParams().height = navBarHeight;
                screenHeight = screenHeight + navBarHeight;
            } else {
                // Landscape
                //noinspection SuspiciousNameCombination
                gapBottom.getLayoutParams().width = navBarHeight;
            }
            gapTop.getLayoutParams().height = statusBarHeight;
        }

        // Title
        if (mTitleTextView.getVisibility() == View.VISIBLE) {
            mTitleTextView.setText(Util.getApplicationNameFromPackage(requestPkg, getActivity()));
            mTitleTextView.setCompoundDrawablesWithIntrinsicBounds(Util.getApplicationIconFromPackage(requestPkg, getActivity()), null, null, null);
            mTitleTextView.setOnLongClickListener(this);
        }

        //Input
        if (mInputBar.getVisibility() == View.VISIBLE) {
            mInputTextView.setText("");
            mDeleteButton.setOnClickListener(this);
            mDeleteButton.setOnLongClickListener(this);
        }

        if (prefs.getBoolean(Common.HIDE_TITLE_BAR, false))
            mTitleTextView.setVisibility(View.GONE);
        if (prefs.getBoolean(Common.HIDE_INPUT_BAR, false))
            mInputBar.setVisibility(View.GONE);
        if (prefs.getBoolean(Common.INVERT_COLOR, false)) {
            mTitleTextView.setTextColor(Color.BLACK);
            mInputTextView.setTextColor(Color.BLACK);
            mDeleteButton.setColorFilter(android.R.color.black, PorterDuff.Mode.SRC_ATOP);
        }

        if (!lockingType.equals(Common.PREF_VALUE_KNOCK_CODE) && isTablet()) {
            // Header views
            LinearLayout.LayoutParams title = new LinearLayout.LayoutParams(mTitleTextView.getLayoutParams());
            title.setMargins(getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_bottom), getDimens(R.dimen.tablet_margin_sides), 0);
            mTitleTextView.setLayoutParams(title);
            LinearLayout.LayoutParams input = new LinearLayout.LayoutParams(rootView.findViewById(R.id.input_bar).getLayoutParams());
            input.setMargins(getDimens(R.dimen.tablet_margin_sides), 0, getDimens(R.dimen.tablet_margin_sides), 0);
            rootView.findViewById(R.id.input_bar).setLayoutParams(input);
            // Container
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) container.getLayoutParams();
            params.setMargins(getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_top), getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_bottom));
            params.setMargins(getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_top), getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_bottom));
            container.setLayoutParams(params);
        }
        return rootView;
    }

    private int getDimens(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }

    private boolean isTablet() {
        return prefs.getBoolean(Common.OVERRIDE_TABLET_MODE, getResources().getBoolean(R.bool.tablet_mode_default));
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                kCCBackground = Bitmap.createBitmap(container.getWidth(), container.getHeight(), Bitmap.Config.ARGB_8888);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN && lockingType.equals(Common.PREF_VALUE_KNOCK_CODE)) {
                    container.setBackgroundDrawable(new BitmapDrawable(getResources(), kCCBackground));
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    container.setBackground(new BitmapDrawable(getResources(), kCCBackground));
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                // Center values
                int[] loc = new int[2];
                container.getLocationOnScreen(loc);
                containerX = loc[0];
                containerY = loc[1];
            }
        });
    }

    private void setupPINLayout() {
        for (int i : pinButtonIds) {
            View pb = rootView.findViewById(i);
            pb.setOnClickListener(this);
            pb.setOnLongClickListener(this);
            if (prefs.getBoolean(Common.INVERT_COLOR, false)) {
                //noinspection deprecation
                ((TextView) pb).setTextColor(getResources().getColor(android.R.color.black));
            }
        }
        if (prefs.getBoolean(Common.ENABLE_QUICK_UNLOCK, false)) {
            rootView.findViewById(R.id.pin_ok).setVisibility(View.INVISIBLE);
        }
    }

    @SuppressWarnings("deprecation")
    private void setupKnockCodeLayout() {
        if (prefs.getBoolean(Common.MAKE_KC_TOUCH_VISIBLE, true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            kCTouchLP = new RippleDrawable(ColorStateList.valueOf(getResources().getColor(R.color.legacy_highlight_dark)), null, new ColorDrawable(Color.WHITE));
            container.setForeground(kCTouchLP);
            kCTouchLP.setState(new int[]{});
        }
        container.setOnLongClickListener(this);
        container.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (prefs.getBoolean(Common.MAKE_KC_TOUCH_VISIBLE, true)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            kCTouchLP.setState(new int[]{android.R.attr.state_pressed});
                            kCTouchLP.setHotspot(e.getRawX(), e.getRawY());
                        } else {
                            kCTouchColor.setShader(new RadialGradient(e.getRawX() - containerX, e.getRawY() - containerY, 200,
                                    getResources().getColor(R.color.legacy_highlight_dark), Color.TRANSPARENT, Shader.TileMode.CLAMP));
                            Canvas c = new Canvas(kCCBackground);
                            c.drawCircle(e.getRawX() - containerX, e.getRawY() - containerY, 100, kCTouchColor);
                            container.invalidate();
                        }
                    }
                    mInputTextView.append("\u2022");

                    // Center values
                    int viewCenterX = containerX + container.getWidth() / 2;
                    int viewCenterY = containerY + container.getHeight() / 2;

                    // Track touch positions
                    knockCodeX.add(e.getRawX());
                    knockCodeY.add(e.getRawY());
                    if (knockCodeX.size() != knockCodeY.size()) {
                        throw new RuntimeException("The amount of the X and Y coordinates doesn't match!");
                    }

                    // Calculate center
                    float centerX;
                    float differenceX = Collections.max(knockCodeX) - Collections.min(knockCodeX);
                    if (differenceX > 50) {
                        centerX = Collections.min(knockCodeX) + differenceX / 2;
                    } else centerX = viewCenterX;

                    float centerY;
                    float differenceY = Collections.max(knockCodeY) - Collections.min(knockCodeY);
                    if (differenceY > 50) {
                        centerY = Collections.min(knockCodeY) + differenceY / 2;
                    } else centerY = viewCenterY;

                    // Calculate key
                    key.setLength(0);
                    for (int i = 0; i < knockCodeX.size(); i++) {
                        float x = knockCodeX.get(i), y = knockCodeY.get(i);
                        if (x < centerX && y < centerY)
                            key.append("1");
                        else if (x > centerX && y < centerY)
                            key.append("2");
                        else if (x < centerX && y > centerY)
                            key.append("3");
                        else if (x > centerX && y > centerY)
                            key.append("4");
                    }
                    checkInput();
                } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (prefs.getBoolean(Common.MAKE_KC_TOUCH_VISIBLE, true) && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        kCCBackground.eraseColor(Color.TRANSPARENT);
                        container.invalidate();
                    }
                }
                return false;
            }
        });
        View divider = new View(getActivity());
        divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(getResources().getDisplayMetrics().density)));
        divider.setBackgroundColor(getResources().getColor(R.color.divider_dark));
        container.addView(divider);
        if (prefs.getBoolean(Common.INVERT_COLOR, false) && prefs.getBoolean(Common.SHOW_KC_DIVIDER, true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                divider.setBackground(getResources().getDrawable(android.R.color.black));
            else
                divider.setBackgroundDrawable(getResources().getDrawable(android.R.color.black));
        } else if (!prefs.getBoolean(Common.SHOW_KC_DIVIDER, true) || screenWidth > screenHeight) {
            divider.setVisibility(View.GONE);
        }
    }

    private void setupPatternLayout() {
        // Pattern View
        lockPatternView = (LockPatternView) rootView.findViewById(R.id.pattern_view);
        // Pattern Listener
        patternListener = new LockPatternView.OnPatternListener() {
            @Override
            public void onPatternStart() {
                lockPatternView.removeCallbacks(mLockPatternViewReloader);
            }

            @Override
            public void onPatternCleared() {
                lockPatternView.removeCallbacks(mLockPatternViewReloader);
                lockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
            }

            @Override
            public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {

            }

            @Override
            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                key.setLength(0);
                key.append(pattern);
                if (!checkInput()) {
                    lockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    lockPatternView.postDelayed(mLockPatternViewReloader, DateUtils.SECOND_IN_MILLIS);
                }
            }
        };
        // Layout
        switch (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
                final int size = getDimens(com.haibison.android.lockpattern.R.dimen.alp_42447968_lockpatternview_size);
                ViewGroup.LayoutParams lp = lockPatternView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                lockPatternView.setLayoutParams(lp);
                break;
            }
        }
        lockPatternView.setOnPatternListener(patternListener);
        lockPatternView.setInStealthMode(!prefs.getBoolean(Common.SHOW_PATTERN_PATH, true));
        lockPatternView.setTactileFeedbackEnabled(prefs.getBoolean(Common.ENABLE_PATTERN_FEEDBACK, true));
    }

    @Override
    public void onClick(View view) {
        if (pinButtonIds.contains(view.getId())) {
            if (view.getId() == R.id.pin_ok) {
                checkInput();
                return;
            }
            String t = ((TextView) view).getText().toString();
            key.append(t);
            mInputTextView.append(t);
            if (prefs.getBoolean(Common.ENABLE_QUICK_UNLOCK, false)) {
                checkInput();
            }
            return;
        }

        switch (view.getId()) {
            case R.id.delete_input:
                if (key.length() > 0) {
                    key.deleteCharAt(key.length() - 1);
                    if (mInputTextView.length() > 0) {
                        mInputTextView.setText(mInputTextView.getText().subSequence(0, mInputTextView.getText().length() - 1));
                    }
                    if (lockingType.equals(Common.PREF_VALUE_KNOCK_CODE) && knockCodeX.size() > 0) {
                        knockCodeX.remove(knockCodeX.size() - 1);
                        knockCodeY.remove(knockCodeY.size() - 1);
                    }
                }
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.title_view:
                Toast.makeText(getActivity(), getArguments().getString("activityName"), Toast.LENGTH_SHORT).show();
                return true;
            default:
                key.setLength(0);
                mInputTextView.setText("");
                if (lockingType.equals(Common.PREF_VALUE_KNOCK_CODE)) {
                    knockCodeX.clear();
                    knockCodeY.clear();
                }
                return true;
        }
    }

    private boolean checkInput() {
        if (Util.shaHash(key.toString()).equals(password) || password.equals("")) {
            key.trimToSize();
            authenticationSucceededListener.onAuthenticationSucceeded();
            return true;
        }
        return false;
    }
}