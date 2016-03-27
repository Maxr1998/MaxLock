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

package de.Maxr1998.xposed.maxlock.preference;

import android.content.Context;
import android.os.Build;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.Maxr1998.xposed.maxlock.R;

public class ExpandingLicensePreference extends Preference {

    private static final String RES_AUTO = "http://schemas.android.com/apk/res-auto";
    private String licenseText = "";
    private LinearLayout view;
    private TextView licenseTextView;
    private View expandIcon;

    public ExpandingLicensePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandingLicensePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        licenseText = context.getString(attrs.getAttributeResourceValue(RES_AUTO, "license", 0));
        setWidgetLayoutResource(R.layout.expanding_license_preference);
    }

    @Override
    protected void onBindView(@NonNull View view) {
        this.view = (LinearLayout) view;
        super.onBindView(view);
        expandIcon = view.findViewById(android.R.id.icon1);
        licenseTextView = (TextView) view.findViewById(android.R.id.text1);
        if (licenseTextView != null) {
            licenseTextView.setText(licenseText);
        }
    }

    @Override
    protected void onClick() {
        switch (licenseTextView.getVisibility()) {
            case View.GONE:
                expandIcon.setVisibility(View.GONE);
                LinearLayout.LayoutParams textContainerOpen = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int endMargin = view.getResources().getDimensionPixelSize(R.dimen.preference_margin);
                textContainerOpen.setMargins(0, 0, endMargin, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    textContainerOpen.setMarginEnd(endMargin);
                }
                view.getChildAt(0).setLayoutParams(textContainerOpen);
                view.setOrientation(LinearLayout.VERTICAL);
                licenseTextView.setVisibility(View.VISIBLE);
                break;
            case View.VISIBLE:
                licenseTextView.setVisibility(View.GONE);
                view.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams textContainerClosed = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
                textContainerClosed.weight = 1;
                view.getChildAt(0).setLayoutParams(textContainerClosed);
                expandIcon.setVisibility(View.VISIBLE);
                break;
        }
    }
}
