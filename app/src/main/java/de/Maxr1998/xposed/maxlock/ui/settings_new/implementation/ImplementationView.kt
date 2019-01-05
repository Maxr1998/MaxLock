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

package de.Maxr1998.xposed.maxlock.ui.settings_new.implementation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.MLImplementation
import de.Maxr1998.xposed.maxlock.MLImplementation.DEFAULT
import de.Maxr1998.xposed.maxlock.MLImplementation.NO_XPOSED
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.prefs

class ImplementationView(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(ctx, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val prefs: SharedPreferences = context.prefs
    private lateinit var group: RadioGroup
    private lateinit var accessibilityAlert: View
    private lateinit var implementationError: TextView

    override fun onFinishInflate() {
        super.onFinishInflate()
        group = findViewById(R.id.implementation_group)
        accessibilityAlert = findViewById(R.id.implementation_alert)
        implementationError = findViewById(R.id.implementation_error)

        val currentImplementation = MLImplementation.getImplementation(prefs)
        group.check(if (currentImplementation == DEFAULT) R.id.implementation_item_default else R.id.implementation_item_no_xposed)
        group.setOnCheckedChangeListener { _, _ ->
            val checkedImplementation = when (group.checkedRadioButtonId) {
                R.id.implementation_item_default -> DEFAULT
                else -> NO_XPOSED
            }
            prefs.edit().putInt(Common.ML_IMPLEMENTATION, checkedImplementation).apply()
            updateView(checkedImplementation)
            if (checkedImplementation == DEFAULT)
                MLImplementation.launchDaemon(context)
        }

        if (!MLImplementation.isAccessibilitySupported) {
            group.visibility = View.GONE
        }

        implementationError.setOnClickListener {
            if (MLImplementation.getImplementation(prefs) == NO_XPOSED)
                try {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
        }
        updateView(currentImplementation)
    }

    private fun updateView(implementation: Int = MLImplementation.getImplementation(prefs)) {
        val defaultChecked = implementation == DEFAULT
        accessibilityAlert.visibility = if (!defaultChecked) View.VISIBLE else View.GONE
        implementationError.setText(if (defaultChecked) R.string.xposed_inactive_warning else R.string.accessibility_inactive_warning)
        implementationError.visibility = if (!MLImplementation.isActiveAndWorking(context, prefs)) View.VISIBLE else View.GONE
        if (MLImplementation.isXposedActive()) {
            findViewById<TextView>(R.id.xposed_success_information).visibility = View.VISIBLE
            for (i in 0 until group.childCount) {
                group.getChildAt(i).isEnabled = false
            }
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        updateView()
    }
}