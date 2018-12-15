/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2018  Max Rumpf alias Maxr1998
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

package de.Maxr1998.xposed.maxlock.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSeekBar;
import de.Maxr1998.xposed.maxlock.R;

/**
 * Preference class implementing a SeekBar to specify in values
 *
 * @author Robobunny, modified by Max Rumpf alias Maxr1998
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private static final String ANDROID_XMLNS = "http://schemas.android.com/apk/res/android";
    private static final String SEEK_BAR_XMLNS = "http://robobunny.com";
    private static final int DEFAULT_VALUE = 50;
    private final String TAG = getClass().getName();
    private int mMaxValue = 100;
    private int mMinValue = 0;
    private int mInterval = 1;
    private int mCurrentValue;
    private AppCompatSeekBar mSeekBar;
    private TextView mStatusText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        mSeekBar = new AppCompatSeekBar(context, attrs);
        mSeekBar.setPadding(0,0,0,0);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);
        setWidgetLayoutResource(R.layout.seek_bar_preference);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mMaxValue = attrs.getAttributeIntValue(ANDROID_XMLNS, "max", 100);
        mMinValue = attrs.getAttributeIntValue(SEEK_BAR_XMLNS, "min", 0);
        try {
            String newInterval = attrs.getAttributeValue(SEEK_BAR_XMLNS, "interval");
            if (newInterval != null)
                mInterval = Integer.parseInt(newInterval);
        } catch (Exception e) {
            Log.e(TAG, "Invalid interval value", e);
        }
    }

    @Override
    protected View onCreateView(ViewGroup v) {
        View view = super.onCreateView(v);
        // The basic preference layout puts the widget frame to the right of the title and summary,
        // so we need to change it a bit - the seekbar should be under them.
        LinearLayout layout = (LinearLayout) view;
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textContainer = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int endMargin = (int) (16 * view.getContext().getResources().getDisplayMetrics().density);
        textContainer.setMargins(0, 0, endMargin, 0);
        textContainer.setMarginEnd(endMargin);
        layout.getChildAt(0).setLayoutParams(textContainer);
        return view;
    }

    @Override
    public void onBindView(@NonNull View view) {
        super.onBindView(view);
        try {
            View summary = view.findViewById(android.R.id.summary);
            ViewParent oldContainer = mSeekBar.getParent();
            ViewGroup newContainer = view.findViewById(R.id.seekBarPrefBarContainer);

            summary.setPadding(0, 0, (int) view.getContext().getResources().getDisplayMetrics().density * 16, 0);
            View textContainer = ((View) summary.getParent());
            textContainer.setPadding(textContainer.getPaddingLeft(), textContainer.getPaddingTop(), textContainer.getPaddingRight(), textContainer.getPaddingBottom() / 2);
            // move our seekbar to the new view we've been given
            if (oldContainer != newContainer) {
                // remove the seekbar from the old view
                if (oldContainer != null) {
                    ((ViewGroup) oldContainer).removeView(mSeekBar);
                }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer.removeAllViews();
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error binding view: " + ex.toString());
        }

        //if dependency is false from the beginning, disable the seek bar
        if (!isEnabled()) {
            mSeekBar.setEnabled(false);
        }
        updateView(view);
    }

    /**
     * Update a SeekBarPreference view with our current state
     */
    private void updateView(View view) {
        try {
            mStatusText = view.findViewById(R.id.seekBarPrefValue);
            mSeekBar.setProgress(mCurrentValue - mMinValue);
            setText();
        } catch (Exception e) {
            Log.e(TAG, "Error updating seek bar preference", e);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = progress + mMinValue;
        if (newValue > mMaxValue)
            newValue = mMaxValue;
        else if (newValue < mMinValue)
            newValue = mMinValue;
        else if (mInterval != 1 && newValue % mInterval != 0)
            newValue = Math.round(((float) newValue) / mInterval) * mInterval;

        // change rejected, revert to the previous value
        if (!callChangeListener(newValue)) {
            seekBar.setProgress(mCurrentValue - mMinValue);
            return;
        }
        // change accepted, store it
        mCurrentValue = newValue;
        setText();
        persistInt(newValue);
    }

    private void setText() {
        String status = String.valueOf(mCurrentValue / 60000);
        if (mCurrentValue % 60000 == 30000) {
            status = status + ":30";
        } else {
            status = status + ":00";
        }
        mStatusText.setText(status);
        mStatusText.setMinimumWidth(30);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }


    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        return ta.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue);
        } else {
            int temp = 0;
            try {
                temp = (Integer) defaultValue;
            } catch (Exception ex) {
                Log.e(TAG, "Invalid default value: " + defaultValue.toString());
            }
            persistInt(temp);
            mCurrentValue = temp;
        }
    }

    /**
     * make sure that the seekbar is disabled if the preference is disabled
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mSeekBar.setEnabled(enabled);
    }

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
        //Disable movement of seek bar when dependency is false
        if (mSeekBar != null) {
            mSeekBar.setEnabled(!disableDependent);
        }
    }
}