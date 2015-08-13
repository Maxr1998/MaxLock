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

package de.Maxr1998.xposed.maxlock.tasker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;

public class ConfigActivity extends AppCompatActivity {

    public static final String STATE_EXTRA_KEY = "de.Maxr1998.xposed.maxlock.extra.STRING_MESSAGE";
    private Intent result;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);

        BundleScrubber.scrub(getIntent());
        final Bundle extra = getIntent().getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        BundleScrubber.scrub(extra);

        setContentView(R.layout.activity_tasker_config);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(getString(R.string.app_name) + " Tasker Plugin");

        if (!prefs.getBoolean(Common.TASKER_ENABLED, false)) {
            findViewById(R.id.tasker_warning).setVisibility(View.VISIBLE);
            findViewById(R.id.tasker_config_main).setVisibility(View.GONE);
            return;
        }

        final RadioGroup options = (RadioGroup) findViewById(R.id.tasker_config_options);
        final Button apply = (Button) findViewById(R.id.tasker_apply_button);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                result = getResultIntent(options.getCheckedRadioButtonId());
                finish();
            }
        });
        if (savedInstanceState == null && extra != null) {
            options.check(extra.getInt(STATE_EXTRA_KEY, -1));
        }
    }

    @Override
    public void finish() {
        if (result != null) {
            setResult(RESULT_OK, result);
        } else setResult(RESULT_CANCELED);
        super.finish();
    }

    private Intent getResultIntent(int resultId) {
        Intent resultIntent = new Intent();
        Bundle resultBundle = new Bundle();
        resultBundle.putInt(STATE_EXTRA_KEY, resultId);
        resultIntent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", resultBundle);
        resultIntent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", ((RadioButton) findViewById(resultId)).getText().toString());
        return resultIntent;
    }
}
