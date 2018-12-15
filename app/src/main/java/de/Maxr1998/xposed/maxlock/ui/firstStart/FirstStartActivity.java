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

package de.Maxr1998.xposed.maxlock.ui.firstStart;

import android.os.Bundle;
import android.view.View;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

import androidx.viewpager.widget.ViewPager;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.firstStart.views.InformationView;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;

public class FirstStartActivity extends IntroActivity {

    public static final int FIRST_START_LATEST_VERSION = 40;
    public static final String FIRST_START_LAST_VERSION_KEY = "first_start_last_version";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setFullscreen(false);
        super.onCreate(savedInstanceState);
        addSlide(new SimpleSlide.Builder()
                .title(R.string.fs_welcome_maxlock)
                .image(R.drawable.ic_welcome)
                .background(R.color.first_start_page1)
                .scrollable(false)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title(R.string.fs_maxlock_description)
                .background(R.color.first_start_page2)
                .layout(R.layout.fs_information)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title(R.string.fs_recommended_apps)
                .description(R.string.fs_recommended_apps_summary)
                .background(R.color.first_start_page3)
                .layout(R.layout.fs_config)
                .build());
        addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                View fragmentView = getItem(1).getView();
                if (fragmentView != null && position == 1) {
                    ((InformationView) fragmentView.findViewById(R.id.information_view)).onViewGotVisible();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void finish() {
        MLPreferences.getPreferences(this).edit().putInt(FIRST_START_LAST_VERSION_KEY, FIRST_START_LATEST_VERSION).apply();
        super.finish();
    }
}