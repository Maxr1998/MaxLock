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

package de.Maxr1998.xposed.maxlock.ui.settings.applist

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.asReference
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class AppListViewHolder(view: View) : RecyclerView.ViewHolder(view), CoroutineScope {

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    var appIcon: ImageView = itemView.findViewById(R.id.icon)
    private val appName: TextView = itemView.findViewById(R.id.title)
    val options: ImageButton = itemView.findViewById(R.id.edit)
    private val toggle: ToggleButton = itemView.findViewById(R.id.toggleLock)

    lateinit var packageName: String
    private val prefsApps: SharedPreferences = MLPreferences.getPrefsApps(itemView.context)

    init {
        // Launch app when tapping icon
        appIcon.setOnClickListener { v ->
            val launch = v.context.packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                v.context.startActivity(launch)
            }
        }

        // Turn lock on/off
        toggle.setOnClickListener { v ->
            val value = (v as ToggleButton).isChecked
            if (value) {
                prefsApps.edit().putBoolean(packageName, true).apply()
                options.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.applist_settings))
                options.visibility = View.VISIBLE
            } else {
                prefsApps.edit().remove(packageName).apply()
                val out = AnimationUtils.loadAnimation(v.getContext(), R.anim.applist_settings_out) as AnimationSet
                out.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}

                    override fun onAnimationEnd(anim: Animation) {
                        val animation = TranslateAnimation(0.0f, 0.0f, 0.0f, 0.0f).apply {
                            duration = 1
                        }
                        options.startAnimation(animation)
                        options.visibility = View.GONE
                        options.clearAnimation()
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
                options.startAnimation(out)
            }
            toggle.contentDescription = v.getContext().getString(if (value) R.string.content_description_applist_toggle_on else
                R.string.content_description_applist_toggle_off, appName.text
            )
        }
    }

    fun bind(app: AppListModel.AppInfo, cache: LruCache<String, Drawable>) {
        packageName = app.packageName
        appIcon.apply {
            if (cache[packageName] != null) {
                setImageDrawable(cache[packageName])
            } else launch {
                val iconRef = appIcon.asReference()
                withContext(Dispatchers.IO) { app.loadIcon() }.also {
                    iconRef().setImageDrawable(it)
                    cache.put(packageName, it)
                }
            }
        }
        appIcon.contentDescription = appIcon.resources.getString(R.string.content_description_applist_icon, app.name)
        appName.text = app.name
        val locked = prefsApps.getBoolean(packageName, false)
        options.visibility = if (locked) View.VISIBLE else View.GONE
        options.contentDescription = options.resources.getString(R.string.content_description_applist_options, app.name)
        toggle.isChecked = locked
        toggle.contentDescription = toggle.resources.getString(
                if (locked) R.string.content_description_applist_toggle_on
                else R.string.content_description_applist_toggle_off, app.name)
    }
}