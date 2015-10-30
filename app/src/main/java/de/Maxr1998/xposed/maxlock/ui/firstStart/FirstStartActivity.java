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

package de.Maxr1998.xposed.maxlock.ui.firstStart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.firstStart.fragments.FragmentAppSetup;
import de.Maxr1998.xposed.maxlock.ui.firstStart.fragments.FragmentInformation;
import de.Maxr1998.xposed.maxlock.ui.firstStart.fragments.FragmentWelcome;

public class FirstStartActivity extends FragmentActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {

    public static final int FIRST_START_LATEST_VERSION = 28;
    public static final String FIRST_START_LAST_VERSION_KEY = "first_start_last_version";
    private FirstStartPagerAdapter mAdapter;
    private ViewPager mPager;
    private Button skipButton, continueButton, doneButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_start);
        mPager = (ViewPager) findViewById(R.id.first_start_pager);
        mAdapter = new FirstStartPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(2);
        mPager.addOnPageChangeListener(this);

        skipButton = (Button) findViewById(R.id.skip_button);
        skipButton.setOnClickListener(this);
        continueButton = (Button) findViewById(R.id.continue_button);
        continueButton.setOnClickListener(this);
        doneButton = (Button) findViewById(R.id.done_button);
        doneButton.setOnClickListener(this);
    }

    private void fadeBottomColor(int colorFrom, int colorTo, float percent) {
        final float[] from = new float[3], to = new float[3], hsv = new float[3];

        Color.colorToHSV(colorFrom, from);
        Color.colorToHSV(colorTo, to);

        hsv[0] = from[0] + (to[0] - from[0]) * percent;
        hsv[1] = from[1] + (to[1] - from[1]) * percent;
        hsv[2] = from[2] + (to[2] - from[2]) * percent;

        findViewById(R.id.first_start_bottom).setBackgroundColor(Color.HSVToColor(hsv));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.skip_button:
            case R.id.done_button:
                leave();
                break;
            case R.id.continue_button:
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // Skip and finish button fading, bottom background color fading, Animation listener
        switch (position) {
            case 0:
                fadeBottomColor(getResources().getColor(R.color.first_start_page1), getResources().getColor(R.color.first_start_page2), positionOffset);
                if (positionOffset < 0.4) {
                    ((FragmentPagerSelected) mAdapter.getItem(1)).onScrollAway();
                }
                break;
            case 1:
                fadeBottomColor(getResources().getColor(R.color.first_start_page2), getResources().getColor(R.color.first_start_page3), positionOffset);
                skipButton.setVisibility(View.VISIBLE);
                continueButton.setVisibility(View.VISIBLE);
                skipButton.setAlpha(1 - positionOffset * 1.5f);
                if (positionOffset > 0) {
                    continueButton.setAlpha(1 - positionOffset * 2f);
                    doneButton.setAlpha(positionOffset);
                    doneButton.setVisibility(View.VISIBLE);
                } else {
                    doneButton.setVisibility(View.GONE);
                }
                if (positionOffset == 0) {
                    ((FragmentPagerSelected) mAdapter.getItem(1)).onSelect();
                } else if (positionOffset > 0.6) {
                    ((FragmentPagerSelected) mAdapter.getItem(1)).onScrollAway();
                }
                break;
            case 2:
                findViewById(R.id.first_start_bottom).setBackgroundColor(getResources().getColor(R.color.first_start_page3));
                skipButton.setVisibility(View.INVISIBLE);
                continueButton.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void leave() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(FIRST_START_LAST_VERSION_KEY, FIRST_START_LATEST_VERSION).apply();
        startActivity(new Intent(FirstStartActivity.this, SettingsActivity.class));
        finish();
    }

    public interface FragmentPagerSelected {
        void onSelect();

        void onScrollAway();
    }

    private static class FirstStartPagerAdapter extends FragmentPagerAdapter {

        private final Fragment[] items = new Fragment[3];

        public FirstStartPagerAdapter(FragmentManager fm) {
            super(fm);
            items[0] = new FragmentWelcome();
            items[1] = new FragmentInformation();
            items[2] = new FragmentAppSetup();
        }

        @Override
        public Fragment getItem(int position) {
            return items[position];
        }

        @Override
        public int getCount() {
            return items.length;
        }
    }
}
