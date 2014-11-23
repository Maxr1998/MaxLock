package de.Maxr1998.xposed.maxlock.ui.lock;

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
import android.widget.ImageButton;
import android.widget.TextView;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class PinFragment extends Fragment implements View.OnClickListener {

    public AuthenticationSucceededListener authenticationSucceededListener;
    ViewGroup rootView;
    String requestPkg;
    View pinMainLayout, mInputView;
    TextView titleView;
    ImageButton mDeleteButton;
    SharedPreferences pref;
    View[] pinButtons;
    TextView pb;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Main Views
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_pin, container, false);
        pinMainLayout = rootView.findViewById(R.id.pin_main_layout);
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
            pb.setOnClickListener(this);
            pb.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    key.setLength(0);
                    mInputText.setText("");
                    return true;
                }
            });
        }
        personalizeUI();

        // Dimens
        int statusBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
        int navBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        if (screenWidth <= 0)
            screenWidth = 1080;
        if (screenHeight <= 0)
            screenHeight = 1920;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            pinMainLayout.setBackground(Util.getResizedBackground(getActivity(), screenWidth, screenHeight));
        } else {
            pinMainLayout.setBackgroundDrawable(Util.getResizedBackground(getActivity(), screenWidth, screenHeight));
        }
        if (getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.ui.LockActivity") || getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.MasterSwitchShortcutActivity")) {
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

    private void personalizeUI() {
        if (pref.getBoolean(Common.HIDE_TITLE_BAR, false))
            titleView.setVisibility(View.GONE);

        if (pref.getBoolean(Common.HIDE_INPUT_BAR, false))
            rootView.findViewById(R.id.input_bar).setVisibility(View.GONE);

        if (pref.getString(Common.BACKGROUND, "").equals("white") || pref.getBoolean(Common.INVERT_COLOR, false)) {
            titleView.setTextColor(getResources().getColor(R.color.black));
            mInputText.setTextColor(getResources().getColor(R.color.black));
            mDeleteButton.setColorFilter(R.color.black, PorterDuff.Mode.SRC_OVER);
        }
    }

    @Override
    public void onClick(View view) {
        for (View v : pinButtons) {
            if (view.getId() == v.getId()) {
                String t = ((TextView) view).getText().toString();
                if (!t.equals(getString(android.R.string.ok))) {
                    key.append(t);
                    mInputText.append(t);
                    /*if (Util.checkInput(key.toString(), Common.KEY_PIN, getActivity(), requestPkg)) {
                        authenticationSucceededListener.onAuthenticationSucceeded();
                    }*/
                } else {
                    if (Util.checkInput(key.toString(), Common.KEY_PIN, getActivity(), requestPkg)) {
                        authenticationSucceededListener.onAuthenticationSucceeded();
                    }
                }
            }
        }
        if (view.getId() == mDeleteButton.getId()) {
            if (key.length() > 0) {
                key.deleteCharAt(key.length() - 1);
                mInputText.setText(key.toString());
            }
        }
    }
}
