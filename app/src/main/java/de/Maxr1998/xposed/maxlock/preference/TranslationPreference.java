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
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

public class TranslationPreference extends Preference {

    private static final String RES_AUTO = "http://schemas.android.com/apk/res-auto";
    String locale;

    public TranslationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TranslationPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        String language = attrs.getAttributeValue(RES_AUTO, "language");
        if (language.startsWith("zh") && language.contains("-")) {
            locale = new Locale("zh").getDisplayName() + " (" + language.substring(language.lastIndexOf("-") + 1) + ")";
        } else {
            locale = new Locale(language).getDisplayName();
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        setTitle(locale);
        return super.onCreateView(parent);
    }
}
