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
import android.widget.TextView;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;


public class KnockCodeSetupFragment extends Fragment implements View.OnClickListener {

    ViewGroup rootView;
    View mInputView;
    Button mCancelButton;
    private String mFirstKey = "";
    private SharedPreferences pref, keysPref;
    private StringBuilder key;
    private String mUiStage = "first";
    private Button mNextButton;
    private TextView mDescView, mInputText;

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Views
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_knock_code_setup, container, false);
        mDescView = (TextView) rootView.findViewById(R.id.kc_description);
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
        mCancelButton = (Button) rootView.findViewById(R.id.button_cancel);
        mCancelButton.setOnClickListener(this);
        mNextButton = (Button) rootView.findViewById(R.id.button_positive);
        mNextButton.setOnClickListener(this);

        // Prefs
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        keysPref = getActivity().getSharedPreferences(Common.PREF_KEYS, Activity.MODE_PRIVATE);

        // Strings
        key = new StringBuilder("");

        updateUi();

        return rootView;
    }

    public void onClick(View v) {
        int nr = 0;
        boolean knockButton = false;
        switch (v.getId()) {
            case R.id.inputView:
                key.setLength(0);
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
            case R.id.button_cancel:
                getActivity().getFragmentManager().beginTransaction().replace(R.id.frame_container, new SettingsFragment()).commit();
                break;
            case R.id.button_positive:
                handleStage();
                break;
        }
        if (knockButton) key.append(nr);
        updateUi();
    }

    public void updateUi() {
        if (mUiStage.equals("first")) {
            mInputText.setText(genPass(key));
            if (key.length() > 3) {
                mDescView.setText(R.string.continue_done);
                mNextButton.setEnabled(true);
            } else if (key.length() > 0) {
                mDescView.setText(R.string.knock_code_too_short);
                mNextButton.setEnabled(false);
            } else {
                mDescView.setText(R.string.choose_knock_code);
                mNextButton.setEnabled(false);
            }
        } else if (mUiStage.equals("second")) {
            mInputText.setText(genPass(key));
            mDescView.setText(R.string.confirm_knock_code);
            mNextButton.setText(android.R.string.ok);
            if (key.length() > 0) mNextButton.setEnabled(true);
        }
    }

    public void handleStage() {
        if (mUiStage.equals("first")) {
            mFirstKey = key.toString();
            key.setLength(0);
            mUiStage = "second";
            updateUi();
        } else if (mUiStage.equals("second")) {
            if (key.toString().equals(mFirstKey)) {
                pref.edit()
                        .putString(Common.LOCKING_TYPE, Common.KEY_KNOCK_CODE)
                        .commit();
                keysPref.edit()
                        .putString(Common.KEY_KNOCK_CODE, Util.sha1Hash(key.toString()))
                        .remove(Common.KEY_PASSWORD)
                        .remove(Common.KEY_PIN)
                        .commit();
            } else {
                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.msg_password_inconsistent), Toast.LENGTH_SHORT).show();
            }
            getActivity().getFragmentManager().beginTransaction().replace(R.id.frame_container, new SettingsFragment()).commit();
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
