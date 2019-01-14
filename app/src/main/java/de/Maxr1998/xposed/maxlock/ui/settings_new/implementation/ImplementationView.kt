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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import de.Maxr1998.xposed.maxlock.MLImplementation
import de.Maxr1998.xposed.maxlock.MLImplementation.ACCESSIBILITY
import de.Maxr1998.xposed.maxlock.MLImplementation.DEFAULT
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.getColorCompat
import de.Maxr1998.xposed.maxlock.util.lazyView
import de.Maxr1998.xposed.maxlock.util.prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import splitties.dimensions.dip
import kotlin.coroutines.CoroutineContext

class ImplementationView(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(ctx, attrs, defStyleAttr), CoroutineScope {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val job = Job()

    private val prefs: SharedPreferences = context.prefs
    private val implementationStatus: TextView by lazyView(R.id.implementation_status)
    private val implementationAlert: TextView by lazyView(R.id.implementation_alert)
    private val implementationError: TextView by lazyView(R.id.implementation_error)

    override fun onFinishInflate() {
        super.onFinishInflate()
        updateView()
        implementationError.setOnClickListener {
            if (MLImplementation.getImplementation(prefs) == ACCESSIBILITY) {
                try {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateView() {
        launch {
            val default = MLImplementation.getImplementationCheckRoot(prefs) == DEFAULT
            if (default) launch(Dispatchers.IO) {
                MLImplementation.launchDaemon(context)
            }
            implementationStatus.apply {
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                        if (default) R.drawable.ic_check_circle_green_24dp
                        else R.drawable.ic_info_orange_24dp,
                        0, 0, 0)
                compoundDrawablePadding = dip(4)
                setText(if (default) R.string.implementation_rooted else R.string.implementation_not_rooted)
                setTextColor(context.getColorCompat(if (default) R.color.success else R.color.accent))
            }
            implementationAlert.isVisible = !default
            implementationError.isVisible = !MLImplementation.isActiveAndWorking(context, prefs)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        updateView()
    }

    override fun onDetachedFromWindow() {
        job.cancel()
        super.onDetachedFromWindow()
    }
}