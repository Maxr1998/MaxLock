package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class LockFragment extends Fragment implements View.OnClickListener {

    public AuthenticationSucceededListener authenticationSucceededListener;
    ViewGroup rootView;
    String requestPkg;
    View mainLayout, mInputView;
    TextView titleView;
    ImageButton mDeleteButton;
    SharedPreferences prefs, prefsKey, prefsPerApp;
    View[] pinButtons, knockButtons, dividers;
    TextView pb;
    boolean customTheme = true;
    private int screenHeight, screenWidth;
    private String password, lockingType;
    private StringBuilder key;
    private TextView mInputText;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(getActivity().getClass().getSimpleName() + "must implement AuthenticationSucceededListener to use this fragment", e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefsKey = getActivity().getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        prefsPerApp = getActivity().getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_PRIVATE);

        // Strings
        requestPkg = getArguments().getString(Common.INTENT_EXTRAS_PKG_NAME);

        if (prefsPerApp.contains(requestPkg))
            password = prefsPerApp.getString(requestPkg + Common.APP_KEY_PREFERENCE, null);
        else password = prefsKey.getString(Common.KEY_PREFERENCE, "");

        lockingType = prefsPerApp.getString(requestPkg, prefs.getString(Common.LOCKING_TYPE, ""));
    }

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Main Views
        rootView = lockingType.equals(Common.PREF_VALUE_PIN) && customTheme ? (ViewGroup) inflater.inflate(R.layout.pin_custom, container, false) : (ViewGroup) inflater.inflate(R.layout.fragment_lock, container, false);
        mainLayout = rootView.findViewById(R.id.lock_main_layout);
        titleView = (TextView) rootView.findViewById(R.id.title_view);
        //titleView.setTextColor(Util.getTextColor(getActivity()));

        // Views
        mInputView = rootView.findViewById(R.id.inputView);
        mInputText = (TextView) mInputView;
        mInputText.setText("");
        key = new StringBuilder("");
        mDeleteButton = (ImageButton) rootView.findViewById(R.id.delete_input);
        mDeleteButton.setOnClickListener(this);
        mDeleteButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                key.setLength(0);
                mInputText.setText("");
                return true;
            }
        });

        // Dimens
        if (Util.noGingerbread()) {
            Point size = new Point();
            getActivity().getWindowManager().getDefaultDisplay().getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        } else {
            screenWidth = 480;
            screenHeight = 800;
        }
        int statusBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
        int navBarHeight = 0;

        if (getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.ui.LockActivity") || getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.ui.MasterSwitchShortcutActivity")) {
            View gapTop = rootView.findViewById(R.id.top_gap);
            View gapBottom = rootView.findViewById(R.id.bottom_gap);
            if (screenHeight > screenWidth) {
                // Portrait
                if (Util.noGingerbread())
                    navBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
                gapBottom.getLayoutParams().height = navBarHeight;
                screenHeight = screenHeight + navBarHeight;
            } else {
                // Landscape
                if (Util.noGingerbread())
                    navBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height_landscape", "dimen", "android"));
                //noinspection SuspiciousNameCombination
                gapBottom.getLayoutParams().width = navBarHeight;
            }
            gapTop.getLayoutParams().height = statusBarHeight;
            System.out.println(screenHeight + "*" + screenWidth);
            System.out.println("StatBar:" + statusBarHeight);
            System.out.println("NavBar:" + navBarHeight);
        }
        // Background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mainLayout.setBackground(Util.getResizedBackground(getActivity(), screenWidth, screenHeight));
        } else {
            //noinspection deprecation
            mainLayout.setBackgroundDrawable(Util.getResizedBackground(getActivity(), screenWidth, screenHeight));
        }
        // Title
        titleView.setText(Util.getApplicationNameFromPackage(requestPkg, getActivity()));
        titleView.setCompoundDrawablesWithIntrinsicBounds(Util.getApplicationIconFromPackage(requestPkg, getActivity()), null, null, null);

        personalizeUI();

        switch (lockingType) {
            case Common.PREF_VALUE_PASSWORD:
                titleView.setVisibility(View.GONE);
                rootView.findViewById(R.id.input_bar).setVisibility(View.GONE);
                Util.askPassword(getActivity());
                break;
            case Common.PREF_VALUE_PIN:
                inflater.inflate(R.layout.pin_field, (ViewGroup) rootView.findViewById(R.id.container));
                setupPINLayout();
                break;
            case Common.PREF_VALUE_KNOCK_CODE:
                inflater.inflate(R.layout.knock_code_field, (ViewGroup) rootView.findViewById(R.id.container));
                setupKnockCodeLayout();
                break;
            default:
                authenticationSucceededListener.onAuthenticationSucceeded();
                break;
        }
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
        if (prefs.getString(Common.BACKGROUND, "").equals("white") || prefs.getBoolean(Common.INVERT_COLOR, false)) {
            for (View v : pinButtons) {
                pb = (TextView) v;
                ((TextView) v).setTextColor(getResources().getColor(R.color.black));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setupKnockCodeLayout() {
        // knock buttons
        knockButtons = new View[]{
                rootView.findViewById(R.id.knock_button_1),
                rootView.findViewById(R.id.knock_button_2),
                rootView.findViewById(R.id.knock_button_3),
                rootView.findViewById(R.id.knock_button_4)
        };
        for (View kb : knockButtons) {
            kb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int nr = 0;
                    boolean knockButton = false;
                    switch (view.getId()) {
                        case R.id.knock_button_1:
                            nr = 1;
                            knockButton = true;
                            break;
                        case R.id.knock_button_2:
                            nr = 2;
                            knockButton = true;
                            break;
                        case R.id.knock_button_3:
                            nr = 3;
                            knockButton = true;
                            break;
                        case R.id.knock_button_4:
                            nr = 4;
                            knockButton = true;
                            break;
                    }
                    if (knockButton) {
                        key.append(nr);
                        mInputText.append("\u2022");
                    }
                    checkInput();
                }
            });
            kb.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    key.setLength(0);
                    mInputText.setText("");
                    return true;
                }
            });
        }

        if (!prefs.getBoolean(Common.TOUCH_VISIBLE, true)) {
            for (View kb : knockButtons) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    kb.setBackground(getResources().getDrawable(R.drawable.transparent_img_button_background));
                else
                    kb.setBackgroundDrawable(getResources().getDrawable(R.drawable.transparent_img_button_background));
            }
        }
        // dividers
        dividers = new View[]{
                rootView.findViewById(R.id.divider1),
                rootView.findViewById(R.id.divider2),
                rootView.findViewById(R.id.divider3),
                rootView.findViewById(R.id.divider4)
        };
        if ((prefs.getString(Common.BACKGROUND, "").equals("white") || prefs.getBoolean(Common.INVERT_COLOR, false)) && prefs.getBoolean(Common.SHOW_DIVIDERS, true)) {
            for (View divider : dividers) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    divider.setBackground(getResources().getDrawable(R.color.black));
                else
                    divider.setBackgroundDrawable(getResources().getDrawable(R.color.black));
            }
        } else if (!prefs.getBoolean(Common.SHOW_DIVIDERS, true)) {
            for (View divider : dividers) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    divider.setBackground(getResources().getDrawable(R.drawable.transparent_img_button_background));
                else
                    divider.setBackgroundDrawable(getResources().getDrawable(R.drawable.transparent_img_button_background));
            }
        }
        if (screenWidth > screenHeight)
            dividers[0].setVisibility(View.GONE);
    }

    private void personalizeUI() {
        if (prefs.getBoolean(Common.HIDE_TITLE_BAR, false))
            titleView.setVisibility(View.GONE);
        if (prefs.getBoolean(Common.HIDE_INPUT_BAR, false))
            rootView.findViewById(R.id.input_bar).setVisibility(View.GONE);
        if (prefs.getString(Common.BACKGROUND, "").equals("white") || prefs.getBoolean(Common.INVERT_COLOR, false)) {
            titleView.setTextColor(getResources().getColor(R.color.black));
            mInputText.setTextColor(getResources().getColor(R.color.black));
            mDeleteButton.setColorFilter(R.color.black, PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == mDeleteButton.getId()) {
            if (key.length() > 0) {
                key.deleteCharAt(key.length() - 1);
                mInputText.setText(key.toString());
            }
        }
    }

    public void checkInput() {
        if (Util.shaHash(key.toString()).equals(password) || password.equals(""))
            authenticationSucceededListener.onAuthenticationSucceeded();
    }
}
