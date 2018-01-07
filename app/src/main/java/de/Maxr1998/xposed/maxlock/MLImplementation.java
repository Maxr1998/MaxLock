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

package de.Maxr1998.xposed.maxlock;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.List;

import de.Maxr1998.xposed.maxlock.util.MLPreferences;

import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;
import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_VISUAL;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

public final class MLImplementation {

    public static final int DEFAULT = 1;
    private static final int NO_XPOSED = 2;

    private MLImplementation() {
    }

    public static int getImplementation(@NonNull SharedPreferences prefs) {
        if (isXposedActive() || !isAccessibilitySupported()) {
            prefs.edit().putInt(Common.ML_IMPLEMENTATION, DEFAULT).apply(); // Force DEFAULT if below N or Xposed is installed and module is activated
        }
        return prefs.getInt(Common.ML_IMPLEMENTATION, isXposedInstalled() ? DEFAULT : NO_XPOSED); // Return NO_XPOSED as default only if Xposed isn't even installed
    }

    public static boolean isXposedInstalled() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = stack.length - 3; i < stack.length; i++) {
            if (stack[i].toString().contains("de.robv.android.xposed.XposedBridge"))
                return true;
        }
        return false;
    }

    public static boolean isAccessibilitySupported() {
        return SDK_INT >= N;
    }

    @Keep
    public static boolean isXposedActive() {
        return false;
    }

    private static boolean isAccessibilityEnabled(Context c) {
        AccessibilityManager manager = (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> infos = manager.getEnabledAccessibilityServiceList(FEEDBACK_GENERIC | FEEDBACK_VISUAL);
        for (int i = 0; i < infos.size(); i++) {
            String packageName = infos.get(i).getResolveInfo().serviceInfo.packageName; // Ugh. Isn't there sth better? Definitely a // TODO
            if (packageName.equals(BuildConfig.APPLICATION_ID)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isActiveAndWorking(Context c, SharedPreferences prefs) {
        return (getImplementation(prefs) == DEFAULT && isXposedActive()) || (getImplementation(prefs) == NO_XPOSED && isAccessibilityEnabled(c));
    }

    public static View createImplementationDialog(final Context c) {
        class ImplementationDialog {

            private SharedPreferences prefs;
            private ViewGroup implementationView;
            private RadioGroup group;
            private View accessibilityAlert;
            private TextView implementationError;

            @SuppressLint("InflateParams")
            private ImplementationDialog() {
                prefs = MLPreferences.getPreferences(c);
                implementationView = (ViewGroup) LayoutInflater.from(c).inflate(R.layout.dialog_implementation, null);
                group = implementationView.findViewById(R.id.implementation_group);
                accessibilityAlert = implementationView.findViewById(R.id.implementation_alert);
                implementationError = implementationView.findViewById(R.id.implementation_error);
                group.check(getImplementation(MLPreferences.getPreferences(c)) == DEFAULT ? R.id.implementation_item_default : R.id.implementation_item_no_xposed);
                group.setOnCheckedChangeListener((radioGroup, i) -> updateView());

                if (!isAccessibilitySupported()) {
                    group.setVisibility(View.GONE);
                }

                implementationError.setOnClickListener(v -> {
                    if (MLImplementation.getImplementation(prefs) == NO_XPOSED)
                        try {
                            c.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                        }
                });

                implementationView.addView(new View(c) {
                    @Override
                    public void onWindowFocusChanged(boolean hasWindowFocus) {
                        super.onWindowFocusChanged(hasWindowFocus);
                        updateView();
                    }

                    @Override
                    public int getVisibility() {
                        return GONE;
                    }
                });
                updateView();
            }

            private void updateView() {
                boolean defaultChecked = group.getCheckedRadioButtonId() == R.id.implementation_item_default;
                prefs.edit().putInt(Common.ML_IMPLEMENTATION, defaultChecked ? DEFAULT : NO_XPOSED).apply();
                accessibilityAlert.setVisibility(!defaultChecked ? View.VISIBLE : View.GONE);
                implementationError.setText(defaultChecked ? R.string.xposed_inactive_warning : R.string.accessibility_inactive_warning);
                implementationError.setVisibility(!isActiveAndWorking(c, prefs) ? View.VISIBLE : View.GONE);
                if (isXposedActive()) {
                    implementationView.findViewById(R.id.xposed_success_information).setVisibility(View.VISIBLE);
                    for (int i = 0; i < group.getChildCount(); i++) {
                        group.getChildAt(i).setEnabled(false);
                    }
                }
            }

            public View getView() {
                return implementationView;
            }
        }
        return new ImplementationDialog().getView();
    }
}