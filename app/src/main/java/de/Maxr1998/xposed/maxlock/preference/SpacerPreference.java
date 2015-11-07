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

package de.Maxr1998.xposed.maxlock.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import de.Maxr1998.xposed.maxlock.R;

/**
 * Preference spacer, to make preferences looking more grouped
 *
 * @author Max Rumpf alias Maxr1998
 */
public class SpacerPreference extends Preference {

    private final boolean end;

    @SuppressLint("WorldReadableFiles")
    public SpacerPreference(Context context, AttributeSet attr) {
        super(context, attr);
        setLayoutResource(R.layout.spacer_preference);
        setEnabled(false);
        setSelectable(false);
        //noinspection deprecation
        end = !context.obtainStyledAttributes(attr, new int[]{R.attr.topShadow}).getBoolean(0, true);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View v = super.getView(convertView, parent);
        if (end) {
            v.findViewById(R.id.top_shadow).setVisibility(View.INVISIBLE);
        }
        return v;
    }
}
