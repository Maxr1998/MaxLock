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
import android.annotation.TargetApi;
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
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.haibison.android.lockpattern.widget.LockPatternView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.Util;

public final class LockFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    private final List<Integer> pinButtonIds = Arrays.asList(
            R.id.pin1, R.id.pin2, R.id.pin3,
            R.id.pin4, R.id.pin5, R.id.pin6,
            R.id.pin7, R.id.pin8, R.id.pin9,
            R.id.pin0, R.id.pin_ok
    );
    private SharedPreferences prefs;
    private String[] mNames;
    private String mPassword, mLockingType;
    private StringBuilder mCurrentKey;
    private AuthenticationSucceededListener authenticationSucceededListener;
    private FingerprintHelper mFingerprintHelper;
    private ViewGroup rootView, mInputBar;
    private TextView mInputTextView;
    private FrameLayout container;
    private ImageView mFingerprintIndicator;
    private int screenHeight, screenWidth, statusBarHeight, navBarHeight;

    private KnockCodeHolder mKnockCodeHolder;
    private LockPatternView lockPatternView;
    private LockPatternView.OnPatternListener patternListener;
    private final Runnable mLockPatternViewReloader = new Runnable() {
        @Override
        public void run() {
            lockPatternView.clearPattern();
            patternListener.onPatternCleared();
        }
    };

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
        mNames = getArguments().getStringArray(Common.INTENT_EXTRAS_NAMES);
        if (mNames == null) {
            mNames = new String[]{"", ""};
        }

        if (prefsPerApp.contains(mNames[0])) {
            mPassword = prefsPerApp.getString(mNames[0] + Common.APP_KEY_PREFERENCE, null);
        } else mPassword = prefsKey.getString(Common.KEY_PREFERENCE, "");

        mLockingType = prefsPerApp.getString(mNames[0], prefs.getString(Common.LOCKING_TYPE, ""));
        mCurrentKey = new StringBuilder(mPassword.length() + 4);
        mFingerprintHelper = new FingerprintHelper();

        // Dimensions
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup mainContainer, Bundle savedInstanceState) {
        // Views
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_lock, mainContainer, false);
        ImageView background = (ImageView) rootView.findViewById(R.id.background);
        TextView mTitleTextView = (TextView) rootView.findViewById(R.id.title_view);
        mInputBar = (ViewGroup) rootView.findViewById(R.id.input_bar);
        mInputTextView = (TextView) rootView.findViewById(R.id.input_view);
        ImageButton mDeleteButton = (ImageButton) rootView.findViewById(R.id.delete_input);
        container = (FrameLayout) rootView.findViewById(R.id.container);
        mFingerprintIndicator = (ImageView) rootView.findViewById(R.id.fingerprint_indicator);

        // Background
        background.setImageDrawable(Util.getBackground(getActivity(), screenWidth, screenHeight));

        // Gaps for Status and Nav bar
        if (!getActivity().getClass().getName().equals(SettingsActivity.class.getName())) {
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

        // Locking type view setup
        switch (mLockingType) {
            case Common.PREF_VALUE_PASSWORD:
            case Common.PREF_VALUE_PASS_PIN:
                setupPasswordLayout();
                break;
            case Common.PREF_VALUE_PIN:
                inflater.inflate(R.layout.pin_field, container);
                setupPINLayout();
                break;
            case Common.PREF_VALUE_KNOCK_CODE:
                mKnockCodeHolder = new KnockCodeHolder();
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

        // Title
        if (prefs.getBoolean(Common.HIDE_TITLE_BAR, false)) {
            mTitleTextView.setVisibility(View.GONE);
        } else {
            mTitleTextView.setText(Util.getApplicationNameFromPackage(mNames[0], getActivity()));
            mTitleTextView.setCompoundDrawablesWithIntrinsicBounds(Util.getApplicationIconFromPackage(mNames[0], getActivity()), null, null, null);
            mTitleTextView.setOnLongClickListener(this);
        }

        //Input
        if (!mLockingType.equals(Common.PREF_VALUE_PASSWORD) && !mLockingType.equals(Common.PREF_VALUE_PASS_PIN)) {
            if (prefs.getBoolean(Common.HIDE_INPUT_BAR, false)) {
                mInputBar.setVisibility(View.GONE);
            } else {
                mInputTextView.setText("");
                mDeleteButton.setOnClickListener(this);
                mDeleteButton.setOnLongClickListener(this);
            }
        }

        if (!mLockingType.equals(Common.PREF_VALUE_KNOCK_CODE) && isTablet()) {
            // Header views
            ((LinearLayout.LayoutParams) mTitleTextView.getLayoutParams()).setMargins(getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_bottom), getDimens(R.dimen.tablet_margin_sides), 0);
            ((LinearLayout.LayoutParams) mInputBar.getLayoutParams()).setMargins(getDimens(R.dimen.tablet_margin_sides), 0, getDimens(R.dimen.tablet_margin_sides), 0);
            // Container
            ((RelativeLayout.LayoutParams) container.getLayoutParams()).setMargins(getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_top), getDimens(R.dimen.tablet_margin_sides), getDimens(R.dimen.tablet_margin_bottom));
        }

        // Fingerprint Indicator
        if (mFingerprintHelper.supported) {
            mFingerprintIndicator.setVisibility(View.VISIBLE);
            handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_draw_on_animation);
        }

        if (prefs.getBoolean(Common.INVERT_COLOR, false)) {
            mTitleTextView.setTextColor(Color.BLACK);
            mInputTextView.setTextColor(Color.BLACK);
            mDeleteButton.setColorFilter(android.R.color.black, PorterDuff.Mode.SRC_ATOP);
        }
        return rootView;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mKnockCodeHolder != null) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressLint("NewApi")
                @SuppressWarnings("deprecation")
                @Override
                public void onGlobalLayout() {
                    // Remove layout listener
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }

                    // Center values
                    int[] loc = new int[2];
                    container.getLocationOnScreen(loc);
                    mKnockCodeHolder.containerX = loc[0];
                    mKnockCodeHolder.containerY = loc[1];

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                        mKnockCodeHolder.setHighlightLegacy();
                    }
                }
            });
        }
    }

    private int getDimens(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }

    private boolean isTablet() {
        return prefs.getBoolean(Common.OVERRIDE_TABLET_MODE, getResources().getBoolean(R.bool.tablet_mode_default));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void handleFingerprintIndicator(@DrawableRes int id) {
        if (getActivity() != null) {
            Drawable fp = getActivity().getDrawable(id);
            if (fp instanceof AnimatedVectorDrawable) {
                mFingerprintIndicator.setImageDrawable(fp);
                ((AnimatedVectorDrawable) fp).start();
            }
        }
    }

    private void setupPasswordLayout() {
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
                        Util.hideKeyboardFromWindow(getActivity(), getView());
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
                mCurrentKey = new StringBuilder(editable.toString());
                if (prefs.getBoolean(Common.ENABLE_QUICK_UNLOCK, false)) {
                    if (checkInput()) {
                        Util.hideKeyboardFromWindow(getActivity(), getView());
                    }
                }
            }
        });
        mInputBar.addView(mInputTextView);
        int dp16 = Util.dpToPx(getActivity(), 16);
        ((LinearLayout.LayoutParams) mInputBar.getLayoutParams()).setMargins(dp16, 0, dp16, 0);

        // Move fingerprint icon next to input
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((ViewGroup) mFingerprintIndicator.getParent()).removeView(mFingerprintIndicator);
            mInputBar.addView(mFingerprintIndicator);
        }
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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

        // Move fingerprint icon to empty field
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((ViewGroup) mFingerprintIndicator.getParent()).removeView(mFingerprintIndicator);
            ((ViewGroup) rootView.findViewById(android.R.id.empty)).addView(mFingerprintIndicator);
            RelativeLayout.LayoutParams params = ((RelativeLayout.LayoutParams) mFingerprintIndicator.getLayoutParams());
            params.bottomMargin = 0;
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
        }
    }

    @SuppressWarnings("deprecation")
    private void setupKnockCodeLayout() {
        assert mKnockCodeHolder != null;
        container.setOnLongClickListener(this);
        container.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mInputTextView.append("\u2022");

                    if (mKnockCodeHolder.mTouchHighlightVisible) {
                        mKnockCodeHolder.onNewHighlight(e.getRawX(), e.getRawY());
                    }

                    // Center values
                    int viewCenterX = mKnockCodeHolder.containerX + container.getWidth() / 2;
                    int viewCenterY = mKnockCodeHolder.containerY + container.getHeight() / 2;

                    // Track touch positions
                    mKnockCodeHolder.knockCodeX.add(e.getRawX());
                    mKnockCodeHolder.knockCodeY.add(e.getRawY());
                    if (mKnockCodeHolder.knockCodeX.size() != mKnockCodeHolder.knockCodeY.size()) {
                        throw new RuntimeException("The amount of the X and Y coordinates doesn't match!");
                    }

                    // Calculate center
                    float centerX;
                    float differenceX = Collections.max(mKnockCodeHolder.knockCodeX) - Collections.min(mKnockCodeHolder.knockCodeX);
                    if (differenceX > 50) {
                        centerX = Collections.min(mKnockCodeHolder.knockCodeX) + differenceX / 2;
                    } else centerX = viewCenterX;

                    float centerY;
                    float differenceY = Collections.max(mKnockCodeHolder.knockCodeY) - Collections.min(mKnockCodeHolder.knockCodeY);
                    if (differenceY > 50) {
                        centerY = Collections.min(mKnockCodeHolder.knockCodeY) + differenceY / 2;
                    } else centerY = viewCenterY;

                    // Calculate key
                    mCurrentKey.setLength(0);
                    for (int i = 0; i < mKnockCodeHolder.knockCodeX.size(); i++) {
                        float x = mKnockCodeHolder.knockCodeX.get(i), y = mKnockCodeHolder.knockCodeY.get(i);
                        if (x < centerX && y < centerY)
                            mCurrentKey.append("1");
                        else if (x > centerX && y < centerY)
                            mCurrentKey.append("2");
                        else if (x < centerX && y > centerY)
                            mCurrentKey.append("3");
                        else if (x > centerX && y > centerY)
                            mCurrentKey.append("4");
                    }
                    checkInput();
                } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (prefs.getBoolean(Common.MAKE_KC_TOUCH_VISIBLE, true) && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        mKnockCodeHolder.highlightLegacy.eraseColor(Color.TRANSPARENT);
                        container.invalidate();
                    }
                }
                return false;
            }
        });
        if (prefs.getBoolean(Common.SHOW_KC_DIVIDER, true) && screenWidth < screenHeight) {
            View divider = new View(getActivity());
            divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(getResources().getDisplayMetrics().density)));
            if (prefs.getBoolean(Common.INVERT_COLOR, false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    divider.setBackground(getResources().getDrawable(android.R.color.black));
                } else {
                    divider.setBackgroundDrawable(getResources().getDrawable(android.R.color.black));
                }
            } else {
                divider.setBackgroundColor(getResources().getColor(R.color.divider_dark));
            }
            container.addView(divider);
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
                mCurrentKey.setLength(0);
                mCurrentKey.append(pattern);
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
            mCurrentKey.append(t);
            mInputTextView.append(t);
            if (prefs.getBoolean(Common.ENABLE_QUICK_UNLOCK, false)) {
                checkInput();
            }
            return;
        }

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
                Toast.makeText(getActivity(), mNames[1], Toast.LENGTH_SHORT).show();
                return true;
            default:
                mCurrentKey.setLength(0);
                mInputTextView.setText("");
                if (mKnockCodeHolder != null) {
                    mKnockCodeHolder.clear(true);
                }
                return true;
        }
    }

    private boolean checkInput() {
        if (Util.shaHash(mCurrentKey.toString()).equals(mPassword) || mPassword.equals("")) {
            mFingerprintHelper.cancel();
            authenticationSucceededListener.onAuthenticationSucceeded();
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintHelper.authenticate();
    }

    @Override
    public void onPause() {
        mFingerprintHelper.cancel();
        super.onPause();
    }

    private final class KnockCodeHolder {
        private final ArrayList<Float> knockCodeX;
        private final ArrayList<Float> knockCodeY;
        private final Paint touchColorLegacy;
        private final boolean mTouchHighlightVisible;
        private final RippleDrawable highlightLP;
        private int containerX, containerY;
        private Bitmap highlightLegacy;

        public KnockCodeHolder() {
            knockCodeX = new ArrayList<>();
            knockCodeY = new ArrayList<>();

            mTouchHighlightVisible = prefs.getBoolean(Common.MAKE_KC_TOUCH_VISIBLE, true);
            if (mTouchHighlightVisible) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //noinspection deprecation
                    highlightLP = new RippleDrawable(ColorStateList.valueOf(getResources().getColor(R.color.legacy_highlight_dark)), null, new ColorDrawable(Color.WHITE));
                    container.setForeground(highlightLP);
                    highlightLP.setState(new int[]{});

                    // Destroy others
                    touchColorLegacy = null;
                } else {
                    touchColorLegacy = new Paint();
                    //noinspection deprecation
                    touchColorLegacy.setColor(getResources().getColor(R.color.legacy_highlight_dark));
                    touchColorLegacy.setStrokeWidth(1);
                    touchColorLegacy.setStyle(Paint.Style.FILL_AND_STROKE);

                    // Destroy others
                    highlightLP = null;
                }
            } else {
                // Destroy others
                highlightLP = null;
                touchColorLegacy = null;
            }
        }

        public void setHighlightLegacy() {
            highlightLegacy = Bitmap.createBitmap(container.getWidth(), container.getHeight(), Bitmap.Config.ARGB_8888);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                //noinspection deprecation
                container.setBackgroundDrawable(new BitmapDrawable(getResources(), highlightLegacy));
            } else {
                container.setBackground(new BitmapDrawable(getResources(), highlightLegacy));
            }
        }

        public void onNewHighlight(float x, float y) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                highlightLP.setState(new int[]{android.R.attr.state_pressed});
                highlightLP.setHotspot(x, y);
            } else {
                //noinspection deprecation
                touchColorLegacy.setShader(new RadialGradient(x - containerX, y - containerY, 200,
                        getResources().getColor(R.color.legacy_highlight_dark), Color.TRANSPARENT, Shader.TileMode.CLAMP));
                Canvas c = new Canvas(highlightLegacy);
                c.drawCircle(x - containerX, y - containerY, 100, touchColorLegacy);
                container.invalidate();
            }
        }

        public void clear(boolean full) {
            if (full) {
                knockCodeX.clear();
                knockCodeY.clear();
            } else if (knockCodeX.size() > 0) {
                knockCodeX.remove(knockCodeX.size() - 1);
                knockCodeY.remove(knockCodeY.size() - 1);
            }
        }
    }

    private final class FingerprintHelper {
        private final boolean supported;
        private final FingerprintManagerCompat mFingerprintManager;
        private CancellationSignal mCancelFingerprint;
        private FingerprintManagerCompat.AuthenticationCallback mFPAuthenticationCallback;

        public FingerprintHelper() {
            mFingerprintManager = FingerprintManagerCompat.from(getActivity());
            supported = mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints();
            if (!supported) {
                return;
            }
            mFPAuthenticationCallback = new FingerprintManagerCompat.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                    Util.hideKeyboardFromWindow(getActivity(), getView());
                    handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_draw_off_animation);
                    authenticationSucceededListener.onAuthenticationSucceeded();
                }

                @Override
                public void onAuthenticationFailed() {
                    handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_fp_to_error_state_animation);
                    mFingerprintIndicator.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            handleFingerprintIndicator(R.drawable.lockscreen_fingerprint_error_state_to_fp_animation);
                        }
                    }, 800);
                }
            };
            mCancelFingerprint = new CancellationSignal();
        }

        public void authenticate() {
            if (supported) {
                if (mCancelFingerprint.isCanceled()) {
                    mCancelFingerprint = new CancellationSignal();
                }
                mFingerprintManager.authenticate(null, 0, mCancelFingerprint, mFPAuthenticationCallback, null);
            }
        }

        public void cancel() {
            if (mCancelFingerprint != null) {
                mCancelFingerprint.cancel();
            }
        }
    }
}