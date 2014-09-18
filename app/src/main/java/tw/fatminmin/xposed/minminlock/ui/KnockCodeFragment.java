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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import tw.fatminmin.xposed.minminlock.AuthenticationSucceededListener;
import tw.fatminmin.xposed.minminlock.Common;
import tw.fatminmin.xposed.minminlock.R;
import tw.fatminmin.xposed.minminlock.Util;

public class KnockCodeFragment extends Fragment {

    public AuthenticationSucceededListener authenticationSucceededListener;
    private ViewGroup rootView;
    private StringBuilder key;
    private View tvv;
    private TextView tv;

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
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_knock_code, container, false);
        if (getActivity().getActionBar() != null)
            getActivity().getActionBar().hide();

        View kcMainLayout = rootView.findViewById(R.id.kc_main_layout);
        kcMainLayout.setBackground(WallpaperManager.getInstance(getActivity()).getDrawable());

        TextView kcAppName = (TextView) rootView.findViewById(R.id.kc_app_name);

        if (getActivity().getActionBar() == null) {
            LinearLayout.LayoutParams paramsTop = (LinearLayout.LayoutParams) kcAppName.getLayoutParams();
            paramsTop.setMargins(16, 41, 16, 0);
            kcAppName.setLayoutParams(paramsTop);

            LinearLayout bottomLayout = (LinearLayout) rootView.findViewById(R.id.kc_bottom_layout);
            LinearLayout.LayoutParams paramsBottom = (LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
            paramsBottom.setMargins(0, 0, 0, getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android")));
            bottomLayout.setLayoutParams(paramsBottom);
        }

        PackageManager pm = getActivity().getApplicationContext().getPackageManager();
        ApplicationInfo info;

        try {
            info = pm.getApplicationInfo(Common.REQUEST_PKG, 0);
        } catch (PackageManager.NameNotFoundException e) {
            info = null;
        }
        String requestPkgFullName = (String) (info != null ? pm.getApplicationLabel(info) : "(unknown)");

        if (info != null) {
            Drawable requestPkgAppIcon = pm.getApplicationIcon(info);
            kcAppName.setCompoundDrawables(requestPkgAppIcon, null, null, null);
        }

        kcAppName.setTextColor(getResources().getColor(R.color.white));
        kcAppName.setText(requestPkgFullName);
        key = new StringBuilder("");

        tvv = rootView.findViewById(R.id.textView);
        tv = (TextView) tvv;
        tv.setTextColor(getResources().getColor(R.color.white));
        tvv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                key.setLength(0);
                tv.setText(tvc(key));

            }
        });

        View[] knockButtons = new View[]{
                rootView.findViewById(R.id.knock_button_1),
                rootView.findViewById(R.id.knock_button_2),
                rootView.findViewById(R.id.knock_button_3),
                rootView.findViewById(R.id.knock_button_4)
        };
        Button[] kb = new Button[knockButtons.length];

        for (int i = 0; i < knockButtons.length; i++) {
            kb[i] = (Button) knockButtons[i];
            kb[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int nr;
                    switch (view.getId()) {
                        case R.id.knock_button_1:
                            nr = 1;
                            break;
                        case R.id.knock_button_2:
                            nr = 2;
                            break;
                        case R.id.knock_button_3:
                            nr = 3;
                            break;
                        case R.id.knock_button_4:
                            nr = 4;
                            break;
                        default:
                            nr = 0;
                            break;
                    }
                    key.append(nr);
                    tv.setText(tvc(key));
                    if (Util.checkInput(key.toString(), Common.KEY_KNOCK_CODE, getActivity())) {
                        authenticationSucceededListener.onAuthenticationSucceeded(Common.TAG_KCF);
                    }
                }
            });
        }
        return rootView;
    }


    String tvc(StringBuilder str) {
        StringBuilder x = new StringBuilder("");

        for (int i = 0; i < str.length(); i++) {
            x.append("\u2022");
        }
        return x.toString();
    }
}
