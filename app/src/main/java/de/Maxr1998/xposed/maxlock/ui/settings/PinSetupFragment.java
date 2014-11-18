package de.Maxr1998.xposed.maxlock.ui.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class PinSetupFragment extends Fragment implements View.OnClickListener {

    ViewGroup rootView;
    EditText setupPinInput;
    Button setupPinButton;
    private SharedPreferences pref, keysPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // Prefs
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        keysPref = getActivity().getSharedPreferences(Common.PREF_KEYS, Activity.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_pin_setup, container, false);
        setupPinInput = (EditText) rootView.findViewById(R.id.setup_pin_input);
        setupPinButton = (Button) rootView.findViewById(R.id.setup_pin_done);
        setupPinButton.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.setup_pin_done) {
            if (!setupPinInput.getText().equals("")) {
                pref.edit()
                        .putString(Common.LOCKING_TYPE, Common.KEY_PIN)
                        .commit();
                keysPref.edit()
                        .putString(Common.KEY_PIN, Util.shaHash(setupPinInput.getText().toString()))
                        .remove(Common.KEY_PASSWORD)
                        .remove(Common.KEY_KNOCK_CODE)
                        .commit();
                getFragmentManager().popBackStack();
            } else
                Toast.makeText(getActivity(), "PIN should not be empty", Toast.LENGTH_SHORT).show();
        }
    }
}
