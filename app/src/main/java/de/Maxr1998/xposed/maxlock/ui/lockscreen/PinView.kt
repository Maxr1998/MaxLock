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

package de.Maxr1998.xposed.maxlock.ui.lockscreen

import android.annotation.SuppressLint
import android.content.Context
import android.view.HapticFeedbackConstants.*
import android.view.View
import android.view.ViewManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.gridlayout.widget.GridLayout
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.R

@SuppressLint("ViewConstructor")
class PinView(ctx: Context, private val lockView: LockView) : GridLayout(ctx), View.OnClickListener, View.OnLongClickListener {
    private val values = mutableListOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val hapticFeedback = lockView.prefs.getBoolean(Common.ENABLE_PATTERN_FEEDBACK, true)

    init {
        columnCount = 3
        rowCount = 4
        if (lockView.prefs.getBoolean(Common.SHUFFLE_PIN, false)) {
            values.shuffle()
        }
        var j = 0
        for (i in 0 until 12) {
            val v = when (i) {
                9 -> lockView.findViewById<FrameLayout>(R.id.fingerprint_stub).also {
                    (it.parent as ViewManager).removeView(it)
                }
                11 -> AppCompatImageView(ContextThemeWrapper(context, R.style.Widget_TextView_NumPadKey)).apply {
                    setImageResource(R.drawable.ic_checked_24dp)
                    scaleType = ImageView.ScaleType.CENTER
                    setOnClickListener(this@PinView)
                    setOnLongClickListener(this@PinView)
                }
                else -> AppCompatTextView(ContextThemeWrapper(context, R.style.Widget_TextView_NumPadKey)).apply {
                    text = values[j++]
                    setOnClickListener(this@PinView)
                    setOnLongClickListener(this@PinView)
                }
            }
            val params = GridLayout.LayoutParams(GridLayout.spec(i / 3, 1f), GridLayout.spec(i % 3, 1f))
            addView(v, params)
        }
    }

    override fun onClick(v: View) {
        if (hapticFeedback) {
            performHapticFeedback(VIRTUAL_KEY, FLAG_IGNORE_VIEW_SETTING or FLAG_IGNORE_GLOBAL_SETTING)
        }
        if (v is AppCompatTextView) {
            val value = v.text
            if (value.length == 1) {
                lockView.setKey(value.toString(), true)
                lockView.appendToInput(value.toString())
            }
            if (lockView.prefs.getBoolean(Common.ENABLE_QUICK_UNLOCK, false))
                lockView.checkInput()
        } else if (!lockView.checkInput()) {
            lockView.setKey(null, false)
            if (hapticFeedback) {
                handler.postDelayed({ performHapticFeedback(VIRTUAL_KEY, FLAG_IGNORE_VIEW_SETTING or FLAG_IGNORE_GLOBAL_SETTING) }, 120)
            }
            v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
            lockView.handleFailedAttempt()
        }
    }

    override fun onLongClick(v: View): Boolean {
        lockView.setKey(null, false)
        if (hapticFeedback)
            performHapticFeedback(LONG_PRESS, FLAG_IGNORE_VIEW_SETTING or FLAG_IGNORE_GLOBAL_SETTING)
        return true
    }
}