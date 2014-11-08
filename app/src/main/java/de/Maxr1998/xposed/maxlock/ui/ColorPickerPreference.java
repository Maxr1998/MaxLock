package de.Maxr1998.xposed.maxlock.ui;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

public class ColorPickerPreference extends Preference implements Preference.OnPreferenceClickListener {

    public View mView;

    public ColorPickerPreference(Context context) {
        super(context);
        init(context);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setOnPreferenceClickListener(this);
    }


    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mView = view;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}

