package tw.fatminmin.xposed.minminlock.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import tw.fatminmin.xposed.minminlock.R;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
