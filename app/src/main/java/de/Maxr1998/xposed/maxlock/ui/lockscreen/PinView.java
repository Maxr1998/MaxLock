/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2016 Max Rumpf alias Maxr1998
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

package de.Maxr1998.xposed.maxlock.ui.lockscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayout;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;

import static android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING;
import static android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING;
import static android.view.HapticFeedbackConstants.LONG_PRESS;
import static android.view.HapticFeedbackConstants.VIRTUAL_KEY;

@SuppressLint("ViewConstructor")
public class PinView extends GridLayout implements View.OnClickListener, View.OnLongClickListener {

    private final LockView mLockView;
    private final List<String> values = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"));
    private final boolean hapticFeedback;
    private String okString;

    public PinView(Context context, LockView lockView) {
        super(context);
        mLockView = lockView;
        setColumnCount(3);
        setRowCount(4);
        if (mLockView.getPrefs().getBoolean(Common.SHUFFLE_PIN, false)) {
            Collections.shuffle(values);
        }
        values.add(9, null);
        if (!mLockView.getPrefs().getBoolean(Common.ENABLE_QUICK_UNLOCK, false)) {
            okString = getResources().getString(android.R.string.ok);
            if (okString.length() > 4) {
                okString = "OK";
            }
            values.add(okString);
        }

        for (int i = 0; i < values.size(); i++) {
            View v;
            if (values.get(i) != null) {
                PinDigit p = new PinDigit(new ContextThemeWrapper(getContext(), R.style.Widget_TextView_NumPadKey));
                p.setValue(values.get(i));
                p.setOnClickListener(this);
                if (values.get(i).length() == 1) {
                    p.setOnLongClickListener(this);
                }
                v = p;
            } else {
                mLockView.removeView(mLockView.findViewById(R.id.fingerprint_stub));
                v = new FrameLayout(getContext());
                v.setId(R.id.fingerprint_stub);
            }
            LayoutParams params = new LayoutParams(spec(i / 3, 1f), spec(i % 3, 1f));
            addView(v, params);
        }
        hapticFeedback = mLockView.getPrefs().getBoolean(Common.ENABLE_PATTERN_FEEDBACK, true);
    }

    @Override
    public void onClick(View v) {
        if (hapticFeedback) {
            performHapticFeedback(VIRTUAL_KEY, FLAG_IGNORE_VIEW_SETTING | FLAG_IGNORE_GLOBAL_SETTING);
        }
        String value = ((PinDigit) v).getValue();
        if (value.length() == 1) {
            mLockView.setKey(value, true);
            mLockView.appendToInput(value);
        }
        if (mLockView.getPrefs().getBoolean(Common.ENABLE_QUICK_UNLOCK, false)) {
            mLockView.checkInput();
        } else if (value.equals(okString) && !mLockView.checkInput()) {
            mLockView.setKey(null, false);
            if (hapticFeedback) {
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performHapticFeedback(VIRTUAL_KEY, FLAG_IGNORE_VIEW_SETTING | FLAG_IGNORE_GLOBAL_SETTING);
                    }
                }, 120);
            }
            v.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake));
        }
    }

    @Override
    public boolean onLongClick(View v) {
        mLockView.setKey(null, false);
        if (hapticFeedback)
            performHapticFeedback(LONG_PRESS, FLAG_IGNORE_VIEW_SETTING | FLAG_IGNORE_GLOBAL_SETTING);
        return true;
    }
}
