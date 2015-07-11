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

package de.Maxr1998.xposed.maxlock.ui.FirstStart.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.FirstStart.FirstStartActivity;

public class FragmentInformation extends Fragment implements FirstStartActivity.FragmentPagerSelected {

    ViewGroup rootView;
    View lockIcon;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_first_start_information, container, false);
        lockIcon = rootView.findViewById(R.id.first_start_lock_icon);
        return rootView;
    }

    @Override
    public void onSelect() {
        if (getActivity() != null && lockIcon != null && lockIcon.getVisibility() == View.GONE) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.first_start_lock_fly_in);
            lockIcon.startAnimation(anim);
            lockIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onScrollAway() {
        if (lockIcon != null) {
            lockIcon.setVisibility(View.GONE);
        }
    }
}
