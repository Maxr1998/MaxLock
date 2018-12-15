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

package de.Maxr1998.xposed.maxlock.ui.firstStart.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import de.Maxr1998.xposed.maxlock.R;

public class InformationView extends RelativeLayout {

    private Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.first_start_lock_fly_in);

    public InformationView(Context context) {
        this(context, null);
    }

    public InformationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InformationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus)
            onViewGotVisible();
    }

    public void onViewGotVisible() {
        View lockIcon = findViewById(R.id.first_start_lock_icon);
        if (lockIcon.getAnimation() == null) {
            lockIcon.setVisibility(GONE);
            anim.reset();
            lockIcon.postDelayed(() -> {
                lockIcon.startAnimation(anim);
                lockIcon.setVisibility(VISIBLE);
            }, 800);
        }
    }
}