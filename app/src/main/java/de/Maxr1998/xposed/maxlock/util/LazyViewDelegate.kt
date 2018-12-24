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

@file:Suppress("NOTHING_TO_INLINE")

package de.Maxr1998.xposed.maxlock.util

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes

inline fun <V : View> View.lazyView(@IdRes id: Int): Lazy<V> {
    return LazyView({ this }, id)
}

inline fun <V : View> Activity.lazyView(@IdRes id: Int): Lazy<V> {
    return LazyView({ window.decorView }, id)
}

class LazyView<V : View>(private val getRoot: () -> View, @IdRes private val id: Int) : Lazy<V> {
    private var cached: V? = null

    override val value: V
        get() = cached ?: getRoot().findViewById<V>(id).also { cached = it }

    override fun isInitialized() = cached != null
}