package de.Maxr1998.xposed.maxlock.ui.lock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class KnockCodeFragment extends Fragment implements View.OnClickListener {

    public AuthenticationSucceededListener authenticationSucceededListener;
    ViewGroup rootView;
    View kcMainLayout, mInputView;
    TextView titleView;
    ImageButton mDeleteButton;
    SharedPreferences pref;
    String requestPkg;
    Button[] kb;
    View[] dividers, knockButtons;

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
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Strings
        requestPkg = getArguments().getString(Common.INTENT_EXTRAS_PKG_NAME);
    }

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Main Views
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_knock_code, container, false);
        kcMainLayout = rootView.findViewById(R.id.kc_main_layout);
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
        knockButtons = new View[]{
                rootView.findViewById(R.id.knock_button_1),
                rootView.findViewById(R.id.knock_button_2),
                rootView.findViewById(R.id.knock_button_3),
                rootView.findViewById(R.id.knock_button_4)
        };
        kb = new Button[knockButtons.length];
        for (int i = 0; i < knockButtons.length; i++) {
            kb[i] = (Button) knockButtons[i];
            kb[i].setOnClickListener(this);
        }

        personalizeUI();

        // Dimens
        int statusBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
        int navBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            kcMainLayout.setBackground(Util.getResizedBackground(getActivity(), screenWidth, screenHeight));
        } else {
            kcMainLayout.setBackgroundDrawable(Util.getResizedBackground(getActivity(), screenWidth, screenHeight));
        }
        if (getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.ui.LockActivity")) {
            View gapTop = rootView.findViewById(R.id.top_gap);
            View gapBottom = rootView.findViewById(R.id.bottom_gap);
            if (screenWidth < screenHeight) {
                gapTop.getLayoutParams().height = statusBarHeight;
                gapBottom.getLayoutParams().height = navBarHeight;
            } else if (screenWidth > screenHeight) {
                gapTop.getLayoutParams().width = statusBarHeight;
                gapBottom.getLayoutParams().width = navBarHeight;
            }
        }
        titleView.setText(Util.getApplicationNameFromPackage(requestPkg, getActivity()));
        titleView.setCompoundDrawablesWithIntrinsicBounds(Util.getApplicationIconFromPackage(requestPkg, getActivity()), null, null, null);

        return rootView;
    }

    @SuppressLint("NewApi")
    public void personalizeUI() {
        dividers = new View[]{
                rootView.findViewById(R.id.divider1),
                rootView.findViewById(R.id.divider2),
                rootView.findViewById(R.id.divider3),
                rootView.findViewById(R.id.divider4)
        };

        if (pref.getBoolean(Common.HIDE_TITLE_BAR, false))
            titleView.setVisibility(View.GONE);

        if (pref.getBoolean(Common.HIDE_INPUT_BAR, false))
            rootView.findViewById(R.id.input_bar).setVisibility(View.GONE);

        if (pref.getString(Common.KC_BACKGROUND, "").equals("white") || pref.getBoolean(Common.INVERT_COLOR, false)) {
            titleView.setTextColor(getResources().getColor(R.color.black));
            mInputText.setTextColor(getResources().getColor(R.color.black));
            mDeleteButton.setColorFilter(R.color.black, PorterDuff.Mode.SRC_OVER);
        }

        if ((pref.getString(Common.KC_BACKGROUND, "").equals("white") || pref.getBoolean(Common.INVERT_COLOR, false)) && pref.getBoolean(Common.SHOW_DIVIDERS, true)) {
            for (int i = 0; i < dividers.length; i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    dividers[i].setBackground(getResources().getDrawable(R.color.black));
                } else {
                    dividers[i].setBackgroundDrawable(getResources().getDrawable(R.color.black));
                }
            }
        } else if (!pref.getBoolean(Common.SHOW_DIVIDERS, true)) {
            for (int i = 0; i < dividers.length; i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    dividers[i].setBackground(getResources().getDrawable(R.drawable.transparent_img_button_background));
                } else {
                    dividers[i].setBackgroundDrawable(getResources().getDrawable(R.drawable.transparent_img_button_background));
                }
            }
        }

        if (!pref.getBoolean(Common.TOUCH_VISIBLE, true)) {
            for (int i = 0; i < knockButtons.length; i++) {
                kb[i] = (Button) knockButtons[i];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    kb[i].setBackground(getResources().getDrawable(R.drawable.transparent_img_button_background));
                } else {
                    kb[i].setBackgroundDrawable(getResources().getDrawable(R.drawable.transparent_img_button_background));
                }
            }
        }

    }

    public void onClick(View v) {
        int nr = 0;
        boolean knockButton = false;
        switch (v.getId()) {
            case R.id.delete_input:
                if (key.length() > 0) {
                    key.deleteCharAt(key.length() - 1);
                    mInputText.setText(key.toString());
                }
                break;
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
        if (Util.checkInput(key.toString(), Common.KEY_KNOCK_CODE, getActivity(), requestPkg)) {
            authenticationSucceededListener.onAuthenticationSucceeded();
        }
    }
}
