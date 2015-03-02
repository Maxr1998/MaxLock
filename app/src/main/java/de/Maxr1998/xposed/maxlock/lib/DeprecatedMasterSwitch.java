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

package de.Maxr1998.xposed.maxlock.lib;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import de.Maxr1998.xposed.maxlock.ui.MasterSwitchShortcutActivity;

public class DeprecatedMasterSwitch extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent i = new Intent(this, MasterSwitchShortcutActivity.class);
        i.putExtra("LaunchOnly", true);
        startActivity(i);
        finish();
        super.onCreate(savedInstanceState);
    }
}
