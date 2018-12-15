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

package de.Maxr1998.xposed.maxlock.ui.lockscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collections;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;

public class KnockCodeHelper {
    private final ArrayList<Float> knockCodeX;
    private final ArrayList<Float> knockCodeY;
    private final LockView lockView;
    private final FrameLayout container;
    private int containerX, containerY;

    @SuppressLint("ClickableViewAccessibility")
    KnockCodeHelper(LockView lv, FrameLayout c) {
        lockView = lv;
        container = c;
        Context context = container.getContext();

        knockCodeX = new ArrayList<>();
        knockCodeY = new ArrayList<>();

        if (lockView.getPrefs().getBoolean(Common.MAKE_KC_TOUCH_VISIBLE, true)) {
            TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.highlightDrawable});
            container.setForeground(a.getDrawable(0));
            a.recycle();
        }
        container.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                // Center values
                int viewCenterX = containerX + container.getWidth() / 2;
                int viewCenterY = containerY + container.getHeight() / 2;

                // Track touch positions
                knockCodeX.add(e.getRawX());
                knockCodeY.add(e.getRawY());
                if (knockCodeX.size() != knockCodeY.size()) {
                    throw new RuntimeException("The amount of the X and Y coordinates doesn't match!");
                }

                // Calculate center
                float centerX;
                float differenceX = Collections.max(knockCodeX) - Collections.min(knockCodeX);
                if (differenceX > 50) {
                    centerX = Collections.min(knockCodeX) + differenceX / 2;
                } else centerX = viewCenterX;

                float centerY;
                float differenceY = Collections.max(knockCodeY) - Collections.min(knockCodeY);
                if (differenceY > 50) {
                    centerY = Collections.min(knockCodeY) + differenceY / 2;
                } else centerY = viewCenterY;

                // Calculate key
                StringBuilder b = new StringBuilder(5);
                for (int i = 0; i < knockCodeX.size(); i++) {
                    float x = knockCodeX.get(i), y = knockCodeY.get(i);
                    if (x < centerX && y < centerY) {
                        b.append('1');
                    } else if (x > centerX && y < centerY) {
                        b.append('2');
                    } else if (x < centerX && y > centerY) {
                        b.append('3');
                    } else if (x > centerX && y > centerY) {
                        b.append('4');
                    }
                }
                lockView.setKey(b.toString(), false);
                lockView.appendToInput("\u2022");
                lockView.checkInput();
            }
            return false;
        });
        if (lockView.getPrefs().getBoolean(Common.SHOW_KC_DIVIDER, true) && !this.lockView.isLandscape()) {
            View divider = new View(context);
            divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(context.getResources().getDisplayMetrics().density)));
            if (lockView.getPrefs().getBoolean(Common.INVERT_COLOR, false)) {
                divider.setBackground(context.getResources().getDrawable(android.R.color.black));
            } else {
                divider.setBackgroundColor(context.getResources().getColor(R.color.divider_dark));
            }
            container.addView(divider);
        }

        lockView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                // Remove layout listener
                lockView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Center values
                int[] loc = new int[2];
                container.getLocationOnScreen(loc);
                containerX = loc[0];
                containerY = loc[1];
            }
        });
    }

    public void clear(boolean full) {
        if (full) {
            knockCodeX.clear();
            knockCodeY.clear();
        } else if (knockCodeX.size() > 0) {
            knockCodeX.remove(knockCodeX.size() - 1);
            knockCodeY.remove(knockCodeY.size() - 1);
        }
    }
}