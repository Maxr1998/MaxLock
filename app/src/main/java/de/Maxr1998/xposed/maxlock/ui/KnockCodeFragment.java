package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class KnockCodeFragment extends Fragment implements View.OnClickListener {

    public AuthenticationSucceededListener authenticationSucceededListener;
    ViewGroup rootView;
    View kcMainLayout, mInputView;
    TextView kcAppName;
    SharedPreferences pref;
    String requestPkg;
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

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Views
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_knock_code, container, false);
        kcMainLayout = rootView.findViewById(R.id.kc_main_layout);
        kcAppName = (TextView) rootView.findViewById(R.id.kc_app_name);
        mInputView = rootView.findViewById(R.id.inputView);
        mInputView.setOnClickListener(this);
        mInputText = (TextView) mInputView;
        View[] knockButtons = new View[]{
                rootView.findViewById(R.id.knock_button_1),
                rootView.findViewById(R.id.knock_button_2),
                rootView.findViewById(R.id.knock_button_3),
                rootView.findViewById(R.id.knock_button_4)
        };
        Button[] kb = new Button[knockButtons.length];
        for (int i = 0; i < knockButtons.length; i++) {
            kb[i] = (Button) knockButtons[i];
            kb[i].setOnClickListener(this);
        }

        if (!pref.getBoolean(Common.KC_SHOW_DIVIDERS, true)) {
            View[] dividers = new View[]{
                    rootView.findViewById(R.id.divider1),
                    rootView.findViewById(R.id.divider2),
                    rootView.findViewById(R.id.divider3),
                    rootView.findViewById(R.id.divider4)
            };
            View[] d = new View[dividers.length];
            for (int i = 0; i < dividers.length; i++) {
                d[i] = dividers[i];
                d[i].setBackground(getResources().getDrawable(R.drawable.knock_button_background_transparent));
            }
        }

        if (!pref.getBoolean(Common.KC_VISIBLE, true)) {
            for (int i = 0; i < knockButtons.length; i++) {
                kb[i] = (Button) knockButtons[i];
                kb[i].setBackground(getResources().getDrawable(R.drawable.knock_button_background_transparent));
            }
        }

        // Strings
        requestPkg = getArguments().getString(Common.INTENT_EXTRAS_PKG_NAME);
        key = new StringBuilder("");

        // Setup view
        kcMainLayout.setBackground(Util.getBackground(getActivity()));
        if (getActivity().getActionBar() != null)
            getActivity().getActionBar().hide();

        if (getActivity().getActionBar() == null) {
            LinearLayout.LayoutParams paramsTop = (LinearLayout.LayoutParams) kcAppName.getLayoutParams();
            paramsTop.setMargins(getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android")) + 16,
                    getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0);
            kcAppName.setLayoutParams(paramsTop);

            LinearLayout bottomLayout = (LinearLayout) rootView.findViewById(R.id.kc_bottom_layout);
            LinearLayout.LayoutParams paramsBottom = (LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
            paramsBottom.setMargins(getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0,
                    getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android")));
            bottomLayout.setLayoutParams(paramsBottom);
        }

        kcAppName.setText(Util.getApplicationNameFromPackage(requestPkg, getActivity()));
        kcAppName.setCompoundDrawablesWithIntrinsicBounds(Util.getApplicationIconFromPackage(requestPkg, getActivity()), null, null, null);

        return rootView;
    }

    public void onClick(View v) {
        int nr = 0;
        boolean knockButton = false;
        switch (v.getId()) {
            case R.id.inputView:
                key.setLength(0);
                mInputText.setText(genPass(key));
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
        if (knockButton) key.append(nr);
        mInputText.setText(genPass(key));
        if (Util.checkInput(key.toString(), Common.KEY_KNOCK_CODE, getActivity())) {
            authenticationSucceededListener.onAuthenticationSucceeded();
        }
    }


    String genPass(StringBuilder str) {
        StringBuilder x = new StringBuilder("");

        for (int i = 0; i < str.length(); i++) {
            x.append("\u2022");
        }
        return x.toString();
    }
}
