package tw.fatminmin.xposed.minminlock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.WallpaperManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import tw.fatminmin.xposed.minminlock.AuthenticationSucceededListener;
import tw.fatminmin.xposed.minminlock.Common;
import tw.fatminmin.xposed.minminlock.R;
import tw.fatminmin.xposed.minminlock.Util;

public class KnockCodeFragment extends Fragment implements View.OnClickListener {

    public AuthenticationSucceededListener authenticationSucceededListener;
    ViewGroup rootView;
    View kcMainLayout, mInputView;
    TextView kcAppName;
    ApplicationInfo requestPkgInfo;
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

        // Strings
        key = new StringBuilder("");

        // Setup view
        kcMainLayout.setBackground(WallpaperManager.getInstance(getActivity()).getDrawable());
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
        PackageManager pm = getActivity().getApplicationContext().getPackageManager();
        try {
            requestPkgInfo = pm.getApplicationInfo(Common.REQUEST_PKG, 0);
        } catch (PackageManager.NameNotFoundException e) {
            requestPkgInfo = null;
        }
        String requestPkgFullName = (String) (requestPkgInfo != null ? pm.getApplicationLabel(requestPkgInfo) : "(unknown)");
        kcAppName.setText(requestPkgFullName);
        if (requestPkgInfo != null) {
            Drawable requestPkgAppIcon = pm.getApplicationIcon(requestPkgInfo);
            kcAppName.setCompoundDrawablesWithIntrinsicBounds(requestPkgAppIcon, null, null, null);
        }

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
            authenticationSucceededListener.onAuthenticationSucceeded(Common.TAG_KCF);
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
