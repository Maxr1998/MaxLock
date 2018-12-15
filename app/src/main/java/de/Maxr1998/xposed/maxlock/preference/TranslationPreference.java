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

package de.Maxr1998.xposed.maxlock.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import java.util.Locale;

public class TranslationPreference extends Preference {

    private static final String RES_AUTO = "http://schemas.android.com/apk/res-auto";

    public TranslationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        String[] value = attrs.getAttributeValue(RES_AUTO, "language").split("-");
        setTitle(new Locale(value[0], value.length > 1 ? value[1] : "").getDisplayName());
    }
}
