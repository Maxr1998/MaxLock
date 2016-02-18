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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
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
    private final boolean mTouchHighlightVisible;
    private final LockView mLockView;
    private final FrameLayout mContainer;
    private final Context mContext;
    private final Paint touchColorLegacy;
    private final RippleDrawable highlightLP;
    private int containerX, containerY;
    private Bitmap highlightLegacy;

    @SuppressWarnings("deprecation")
    public KnockCodeHelper(LockView lockView, FrameLayout container) {
        mLockView = lockView;
        mContainer = container;
        mContext = mContainer.getContext();

        knockCodeX = new ArrayList<>();
        knockCodeY = new ArrayList<>();

        mTouchHighlightVisible = mLockView.getPrefs().getBoolean(Common.MAKE_KC_TOUCH_VISIBLE, true);
        if (mTouchHighlightVisible) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                highlightLP = new RippleDrawable(ContextCompat.getColorStateList(mContext, R.color.legacy_highlight_dark), null, new ColorDrawable(Color.WHITE));
                mContainer.setForeground(highlightLP);
                highlightLP.setState(new int[]{});

                // Destroy others
                touchColorLegacy = null;
            } else {
                touchColorLegacy = new Paint();
                touchColorLegacy.setColor(ContextCompat.getColor(mContext, R.color.legacy_highlight_dark));
                touchColorLegacy.setStrokeWidth(1);
                touchColorLegacy.setStyle(Paint.Style.FILL_AND_STROKE);

                // Destroy others
                highlightLP = null;
            }
        } else {
            // Destroy others
            highlightLP = null;
            touchColorLegacy = null;
        }
        mContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (mTouchHighlightVisible) {
                        onNewHighlight(e.getRawX(), e.getRawY());
                    }

                    // Center values
                    int viewCenterX = containerX + mContainer.getWidth() / 2;
                    int viewCenterY = containerY + mContainer.getHeight() / 2;

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
                    mLockView.setKey(b.toString(), false);
                    mLockView.appendToInput("\u2022");
                    mLockView.checkInput();
                } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (mLockView.getPrefs().getBoolean(Common.MAKE_KC_TOUCH_VISIBLE, true) && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        highlightLegacy.eraseColor(Color.TRANSPARENT);
                        mContainer.invalidate();
                    }
                }
                return false;
            }
        });
        if (mLockView.getPrefs().getBoolean(Common.SHOW_KC_DIVIDER, true) && !mLockView.isLandscape()) {
            View divider = new View(mContext);
            divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(mContext.getResources().getDisplayMetrics().density)));
            if (mLockView.getPrefs().getBoolean(Common.INVERT_COLOR, false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    divider.setBackground(mContext.getResources().getDrawable(android.R.color.black));
                } else {
                    divider.setBackgroundDrawable(mContext.getResources().getDrawable(android.R.color.black));
                }
            } else {
                divider.setBackgroundColor(mContext.getResources().getColor(R.color.divider_dark));
            }
            mContainer.addView(divider);
        }

        mLockView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                // Remove layout listener
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    mLockView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    mLockView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                // Center values
                int[] loc = new int[2];
                mContainer.getLocationOnScreen(loc);
                containerX = loc[0];
                containerY = loc[1];

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                    setHighlightLegacy();
                }
            }
        });
    }

    public void setHighlightLegacy() {
        highlightLegacy = Bitmap.createBitmap(mContainer.getWidth(), mContainer.getHeight(), Bitmap.Config.ARGB_8888);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            //noinspection deprecation
            mContainer.setBackgroundDrawable(new BitmapDrawable(mContainer.getResources(), highlightLegacy));
        } else {
            mContainer.setBackground(new BitmapDrawable(mContainer.getResources(), highlightLegacy));
        }
    }

    public void onNewHighlight(float x, float y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            highlightLP.setState(new int[]{android.R.attr.state_pressed});
            highlightLP.setHotspot(x, y);
        } else {
            touchColorLegacy.setShader(new RadialGradient(x - containerX, y - containerY, 200,
                    ContextCompat.getColor(mContext, R.color.legacy_highlight_dark), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            Canvas c = new Canvas(highlightLegacy);
            c.drawCircle(x - containerX, y - containerY, 100, touchColorLegacy);
            mContainer.invalidate();
        }
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
