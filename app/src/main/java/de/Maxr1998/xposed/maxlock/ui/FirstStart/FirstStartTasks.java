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

package de.Maxr1998.xposed.maxlock.ui.FirstStart;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class FirstStartTasks extends AsyncTask<Context, Void, Void> {

    @Override
    protected Void doInBackground(Context... contexts) {
        Context context = contexts[0];
        Util.cleanUp(context);

        PreferenceManager.setDefaultValues(context, R.xml.preferences_main, false);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_locking_type, false);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_locking_ui, false);

        return null;
    }
}
