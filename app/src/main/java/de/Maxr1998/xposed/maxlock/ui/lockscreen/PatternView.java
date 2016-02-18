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

package de.Maxr1998.xposed.maxlock.ui.lockscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.ViewGroup;

import com.haibison.android.lockpattern.widget.LockPatternView;

import java.lang.reflect.Field;
import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;

@SuppressLint("ViewConstructor")
public class PatternView extends LockPatternView {

    private final LockView mLockView;
    private final OnPatternListener mPatternListener = new OnPatternListener() {
        @Override
        public void onPatternStart() {
            removeCallbacks(mLockPatternViewReloader);
        }

        @Override
        public void onPatternCleared() {
            removeCallbacks(mLockPatternViewReloader);
            setDisplayMode(LockPatternView.DisplayMode.Correct);
        }

        @Override
        public void onPatternCellAdded(List<Cell> pattern) {

        }

        @Override
        public void onPatternDetected(List<Cell> pattern) {
            mLockView.setPattern(pattern, PatternView.this);

        }
    };
    private final Runnable mLockPatternViewReloader = new Runnable() {
        @Override
        public void run() {
            clearPattern();
            mPatternListener.onPatternCleared();
        }
    };

    public PatternView(Context context, LockView lockView) {
        super(context);
        this.mLockView = lockView;
        setOnPatternListener(mPatternListener);
        switch (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
                final int size = mLockView.getDimens(com.haibison.android.lockpattern.R.dimen.alp_42447968_lockpatternview_size);
                ViewGroup.LayoutParams lp = getLayoutParams();
                lp.width = size;
                lp.height = size;
                break;
            }
        }
        if (mLockView.getPrefs().getBoolean(Common.INVERT_COLOR, false)) {
            try {
                Field field = getClass().getDeclaredField("mRegularColor");
                field.setAccessible(true);
                field.set(this, Color.BLACK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        setInStealthMode(!mLockView.getPrefs().getBoolean(Common.SHOW_PATTERN_PATH, true));
        setTactileFeedbackEnabled(mLockView.getPrefs().getBoolean(Common.ENABLE_PATTERN_FEEDBACK, true));
    }

    public void setWrong() {
        setDisplayMode(LockPatternView.DisplayMode.Wrong);
        postDelayed(mLockPatternViewReloader, DateUtils.SECOND_IN_MILLIS);
    }
}
