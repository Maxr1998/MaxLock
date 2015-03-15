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

package de.Maxr1998.xposed.maxlock.ui.settings.lockingtype;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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
    String customApp;
    Button mCancelButton;
    private String mFirstKey;
    private SharedPreferences prefs, prefsKey, prefsPerApp;
    private StringBuilder key;
    private String mUiStage = "first";
    private Button mNextButton;
    private TextView mDescView, mInputText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // Prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefsKey = getActivity().getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        prefsPerApp = getActivity().getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_PRIVATE);

        // Strings
        key = new StringBuilder("");
        if (getArguments() != null)
            customApp = getArguments().getString(Common.INTENT_EXTRAS_CUSTOM_APP);
    }

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
                getFragmentManager().popBackStack();
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
                if (customApp == null) {
                    prefs.edit().putString(Common.LOCKING_TYPE, Common.PREF_VALUE_KNOCK_CODE).apply();
                    prefsKey.edit().putString(Common.KEY_PREFERENCE, Util.shaHash(key.toString())).apply();
                } else {
                    prefsPerApp.edit().putString(customApp, Common.PREF_VALUE_KNOCK_CODE).putString(customApp + Common.APP_KEY_PREFERENCE, Util.shaHash(key.toString())).apply();
                }
            } else {
                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.msg_password_inconsistent), Toast.LENGTH_SHORT).show();
            }
            getFragmentManager().popBackStack();
            getActivity().onBackPressed();
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