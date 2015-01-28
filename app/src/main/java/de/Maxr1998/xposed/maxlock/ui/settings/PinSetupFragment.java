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

package de.Maxr1998.xposed.maxlock.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class PinSetupFragment extends Fragment implements View.OnClickListener {

    ViewGroup rootView;
    String customApp;
    EditText mSetupPinInput;
    Button mCancelButton;
    private String mFirstKey;
    private String mUiStage = "first";
    private Button mNextButton;
    private TextView mDescView;
    private SharedPreferences prefs, prefsKey, prefsPerApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // Prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefsKey = getActivity().getSharedPreferences(Common.PREFS_KEY, Context.MODE_PRIVATE);
        prefsPerApp = getActivity().getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_PRIVATE);

        // Strings
        if (getArguments() != null)
            customApp = getArguments().getString(Common.INTENT_EXTRAS_CUSTOM_APP);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_pin_setup, container, false);
        mDescView = (TextView) rootView.findViewById(R.id.description);
        mSetupPinInput = (EditText) rootView.findViewById(R.id.setup_pin_input);
        mSetupPinInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                updateUi(charSequence.length());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mSetupPinInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT) {
                    if (mSetupPinInput.getText().length() > 3) {
                        handleStage();
                    }
                    return true;
                }
                return false;
            }
        });
        View mSetupButtons;
        if (prefs.getBoolean(Common.USE_DARK_STYLE, false)) {
            mSetupButtons = inflater.inflate(R.layout.split_button, rootView, false);
        } else {
            mSetupButtons = inflater.inflate(R.layout.split_button_light, rootView, false);
        }
        rootView.addView(mSetupButtons, 2);

        mCancelButton = (Button) rootView.findViewById(R.id.button_cancel);
        mCancelButton.setOnClickListener(this);
        mNextButton = (Button) rootView.findViewById(R.id.button_positive);
        mNextButton.setOnClickListener(this);

        updateUi(0);

        return rootView;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_positive) {
            handleStage();
        } else if (view.getId() == R.id.button_cancel) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mSetupPinInput.getWindowToken(), 0);
            getFragmentManager().popBackStack();
        }
    }

    public void updateUi(int textLength) {
        if (mUiStage.equals("first")) {
            if (textLength > 3) {
                mDescView.setText(R.string.continue_done);
                mNextButton.setEnabled(true);
            } else if (textLength > 0) {
                mDescView.setText(R.string.pin_too_short);
                mNextButton.setEnabled(false);
            } else {
                mDescView.setText(R.string.choose_pin);
                mNextButton.setEnabled(false);
            }
        } else if (mUiStage.equals("second")) {
            mDescView.setText(R.string.confirm_pin);
            mNextButton.setText(android.R.string.ok);
            if (textLength > 0) mNextButton.setEnabled(true);
        }
    }

    public void handleStage() {
        if (mUiStage.equals("first")) {
            mFirstKey = mSetupPinInput.getText().toString();
            mSetupPinInput.setText("");
            mUiStage = "second";
            updateUi(0);
        } else if (mUiStage.equals("second")) {
            if (mSetupPinInput.getText().toString().equals(mFirstKey)) {
                if (customApp == null) {
                    prefs.edit().putString(Common.LOCKING_TYPE, Common.PREF_VALUE_PIN).apply();
                    prefsKey.edit().putString(Common.KEY_PREFERENCE, Util.shaHash(mSetupPinInput.getText().toString())).apply();
                } else {
                    prefsPerApp.edit().putString(customApp, Common.PREF_VALUE_PIN).putString(customApp + Common.APP_KEY_PREFERENCE, Util.shaHash(mSetupPinInput.getText().toString())).apply();
                }
            } else {
                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.msg_password_inconsistent), Toast.LENGTH_SHORT).show();
            }
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mSetupPinInput.getWindowToken(), 0);
            getFragmentManager().popBackStack();
        }
    }
}