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
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.haibison.android.lockpattern.widget.LockPatternView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class LockFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    public AuthenticationSucceededListener authenticationSucceededListener;
    ViewGroup rootView;
    String requestPkg;
    ImageView background;
    View mInputView, container, divider;
    TextView titleView;
    ImageButton mDeleteButton;
    SharedPreferences prefs, prefsKey, prefsPerApp, prefsTheme;
    View[] pinButtons;
    TextView pb;
    LockPatternView lockPatternView;
    LockPatternView.OnPatternListener patternListener;
    private final Runnable mLockPatternViewReloader = new Runnable() {
        @Override
        public void run() {
            lockPatternView.clearPattern();
            patternListener.onPatternCleared();
        }
    };
    int screenHeight, screenWidth;
    private String password, lockingType;
    private StringBuilder key;
    private TextView mInputText;
    private ArrayList<Float> knockCodeX = new ArrayList<>();
    private ArrayList<Float> knockCodeY = new ArrayList<>();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) activity;
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
        prefsKey = getActivity().getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        prefsPerApp = getActivity().getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_PRIVATE);
        //noinspection deprecation
        prefsTheme = getActivity().getSharedPreferences(Common.PREFS_THEME, Context.MODE_WORLD_READABLE);

        // Strings
        requestPkg = getArguments().getString(Common.INTENT_EXTRAS_PKG_NAME);

        if (prefsPerApp.contains(requestPkg))
            password = prefsPerApp.getString(requestPkg + Common.APP_KEY_PREFERENCE, null);
        else password = prefsKey.getString(Common.KEY_PREFERENCE, "");

        lockingType = prefsPerApp.getString(requestPkg, prefs.getString(Common.LOCKING_TYPE, ""));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup mainContainer, Bundle savedInstanceState) {
        // Views
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_lock, mainContainer, false);
        background = (ImageView) rootView.findViewById(R.id.background);
        titleView = (TextView) rootView.findViewById(R.id.title_view);
        mInputView = rootView.findViewById(R.id.input_view);
        mInputText = (TextView) mInputView;
        mInputText.setText("");
        container = rootView.findViewById(R.id.container);
        key = new StringBuilder(50);
        mDeleteButton = (ImageButton) rootView.findViewById(R.id.delete_input);
        mDeleteButton.setOnClickListener(this);
        mDeleteButton.setOnLongClickListener(this);

        // Dimens
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        int statusBarHeight;
        try {
            statusBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            statusBarHeight = 0;
        }
        int navBarHeight = 0;

        if (getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.ui.LockActivity") || getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.ui.MasterSwitchShortcutActivity")) {
            View gapTop = rootView.findViewById(R.id.status_bar_gap);
            View gapBottom = rootView.findViewById(R.id.nav_bar_gap);
            if (screenHeight > screenWidth) {
                // Portrait
                try {
                    navBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                }
                gapBottom.getLayoutParams().height = navBarHeight;
                screenHeight = screenHeight + navBarHeight;
            } else {
                // Landscape
                try {
                    navBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height_landscape", "dimen", "android"));
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                }
                //noinspection SuspiciousNameCombination
                gapBottom.getLayoutParams().width = navBarHeight;
            }
            gapTop.getLayoutParams().height = statusBarHeight;
        }
        // Background
        background.setImageDrawable(Util.getBackground(getActivity(), screenWidth, screenHeight));
        // Title
        titleView.setText(Util.getApplicationNameFromPackage(requestPkg, getActivity()));
        titleView.setCompoundDrawablesWithIntrinsicBounds(Util.getApplicationIconFromPackage(requestPkg, getActivity()), null, null, null);

        if (!lockingType.equals(Common.PREF_VALUE_KNOCK_CODE) && prefs.getBoolean(Common.TABLET_MODE_OVERRIDE, getResources().getBoolean(R.bool.tablet_mode_default))) {
            // Header views
            LinearLayout.LayoutParams title = new LinearLayout.LayoutParams(titleView.getLayoutParams());
            title.setMargins(getResources().getDimensionPixelSize(R.dimen.tablet_margin_sides), getResources().getDimensionPixelSize(R.dimen.tablet_margin_bottom), getResources().getDimensionPixelSize(R.dimen.tablet_margin_sides), 0);
            titleView.setLayoutParams(title);
            LinearLayout.LayoutParams input = new LinearLayout.LayoutParams(rootView.findViewById(R.id.input_bar).getLayoutParams());
            input.setMargins(getResources().getDimensionPixelSize(R.dimen.tablet_margin_sides), 0, getResources().getDimensionPixelSize(R.dimen.tablet_margin_sides), 0);
            rootView.findViewById(R.id.input_bar).setLayoutParams(input);
            // Container
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) container.getLayoutParams();
            params.setMargins(getResources().getDimensionPixelSize(R.dimen.tablet_margin_sides), getResources().getDimensionPixelSize(R.dimen.tablet_margin_top),
                    getResources().getDimensionPixelSize(R.dimen.tablet_margin_sides), getResources().getDimensionPixelSize(R.dimen.tablet_margin_bottom));
            container.setLayoutParams(params);
        }

        personalizeUI();

        switch (lockingType) {
            case Common.PREF_VALUE_PASSWORD:
            case Common.PREF_VALUE_PASS_PIN:
                titleView.setVisibility(View.GONE);
                rootView.findViewById(R.id.input_bar).setVisibility(View.GONE);
                Util.askPassword(getActivity(), password, lockingType.equals(Common.PREF_VALUE_PASS_PIN));
                break;
            case Common.PREF_VALUE_PIN:
                inflater.inflate(R.layout.pin_field, (ViewGroup) container);
                setupPINLayout();
                break;
            case Common.PREF_VALUE_KNOCK_CODE:
                setupKnockCodeLayout();
                break;
            case Common.PREF_VALUE_PATTERN:
                rootView.findViewById(R.id.input_bar).setVisibility(View.GONE);
                inflater.inflate(R.layout.pattern_field, (ViewGroup) container);
                setupPatternLayout();
                break;
            default:
                authenticationSucceededListener.onAuthenticationSucceeded();
                break;
        }
        themeSetup();

        return rootView;
    }

    private void setupPINLayout() {
        pinButtons = new View[]{
                rootView.findViewById(R.id.pin1),
                rootView.findViewById(R.id.pin2),
                rootView.findViewById(R.id.pin3),
                rootView.findViewById(R.id.pin4),
                rootView.findViewById(R.id.pin5),
                rootView.findViewById(R.id.pin6),
                rootView.findViewById(R.id.pin7),
                rootView.findViewById(R.id.pin8),
                rootView.findViewById(R.id.pin9),
                rootView.findViewById(R.id.pin0),
                rootView.findViewById(R.id.pin_ok)
        };
        for (View v : pinButtons) {
            pb = (TextView) v;
            pb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (View v : pinButtons) {
                        if (view.getId() == v.getId()) {
                            String t = ((TextView) view).getText().toString();
                            if (!t.equals(getString(android.R.string.ok))) {
                                key.append(t);
                                mInputText.append(t);
                                if (prefs.getBoolean(Common.QUICK_UNLOCK, false))
                                    checkInput();
                            } else {
                                checkInput();
                            }
                        }
                    }
                }
            });
            pb.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    key.setLength(0);
                    mInputText.setText("");
                    return true;
                }
            });
        }
        if (prefs.getBoolean(Common.INVERT_COLOR, false)) {
            for (View v : pinButtons) {
                pb = (TextView) v;
                ((TextView) v).setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setupKnockCodeLayout() {
        final View container = rootView.findViewById(R.id.container);
        container.setOnLongClickListener(this);
        container.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mInputText.append("\u2022");

                    // Center values
                    int[] loc = new int[2];
                    container.getLocationOnScreen(loc);
                    int viewCenterX = loc[0] + container.getWidth() / 2;
                    int viewCenterY = loc[1] + container.getHeight() / 2;

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
                }
                return false;
            }
        });
        divider = new View(getActivity());
        divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(getResources().getDisplayMetrics().density)));
        divider.setBackgroundColor(getResources().getColor(R.color.light_white));
        ((ViewGroup) container).addView(divider);
        if (prefs.getBoolean(Common.INVERT_COLOR, false) && prefs.getBoolean(Common.KC_SHOW_DIVIDERS, true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                divider.setBackground(getResources().getDrawable(android.R.color.black));
            else
                divider.setBackgroundDrawable(getResources().getDrawable(android.R.color.black));
        } else if (!prefs.getBoolean(Common.KC_SHOW_DIVIDERS, true) || screenWidth > screenHeight) {
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
                final int size = getResources().getDimensionPixelSize(com.haibison.android.lockpattern.R.dimen.alp_42447968_lockpatternview_size);
                ViewGroup.LayoutParams lp = lockPatternView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                lockPatternView.setLayoutParams(lp);
                break;
            }
        }
        lockPatternView.setOnPatternListener(patternListener);
        lockPatternView.setInStealthMode(!prefs.getBoolean(Common.PATTERN_SHOW_PATH, true));
        lockPatternView.setTactileFeedbackEnabled(prefs.getBoolean(Common.PATTERN_FEEDBACK, true));
    }

    private void personalizeUI() {
        if (prefsTheme.getBoolean(Common.HIDE_TITLE_BAR, prefs.getBoolean(Common.HIDE_TITLE_BAR, false)))
            titleView.setVisibility(View.GONE);
        if (prefsTheme.getBoolean(Common.HIDE_INPUT_BAR, prefs.getBoolean(Common.HIDE_INPUT_BAR, false)))
            rootView.findViewById(R.id.input_bar).setVisibility(View.GONE);
        if (prefsTheme.getBoolean(Common.INVERT_COLOR, prefs.getBoolean(Common.INVERT_COLOR, false))) {
            titleView.setTextColor(getResources().getColor(android.R.color.black));
            mInputText.setTextColor(getResources().getColor(android.R.color.black));
            mDeleteButton.setColorFilter(android.R.color.black, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public void themeSetup() {
        if (!prefsTheme.contains(Common.THEME_PKG))
            return;
        container.setLayoutParams(ThemeService.container(container, getActivity(), lockingType));

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == mDeleteButton.getId()) {
            if (key.length() > 0) {
                key.deleteCharAt(key.length() - 1);
                if (mInputText.length() > 0) {
                    mInputText.setText(mInputText.getText().subSequence(0, mInputText.getText().length() - 1));
                }
                if (lockingType.equals(Common.PREF_VALUE_KNOCK_CODE) && knockCodeX.size() > 0) {
                    knockCodeX.remove(knockCodeX.size() - 1);
                    knockCodeY.remove(knockCodeY.size() - 1);
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.container || view.getId() == R.id.delete_input) {
            key.setLength(0);
            mInputText.setText("");
            if (lockingType.equals(Common.PREF_VALUE_KNOCK_CODE)) {
                knockCodeX.clear();
                knockCodeY.clear();
            }
            return true;
        }
        return false;
    }

    public boolean checkInput() {
        if (Util.shaHash(key.toString()).equals(password) || password.equals("")) {
            key.trimToSize();
            authenticationSucceededListener.onAuthenticationSucceeded();
            return true;
        }
        return false;
    }
}