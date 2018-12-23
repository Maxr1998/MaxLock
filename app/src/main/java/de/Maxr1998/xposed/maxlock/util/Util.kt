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
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.media.ThumbnailUtils.OPTIONS_RECYCLE_INPUT
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.withStyledAttributes
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.Common.SETTINGS_PACKAGE_NAME
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.Util.PATTERN_CODE
import de.Maxr1998.xposed.maxlock.util.Util.PATTERN_CODE_APP
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

val Context.applicationName
    get() = StringBuilder(getString(R.string.app_name)).apply {
        if (Util.isDevMode()) {
            append(" Indev")
        } else if (prefs.getBoolean(Common.ENABLE_PRO, false)) {
            append(" ").append(getString(if (prefs.getBoolean(Common.DONATED, false)) R.string.name_pro else R.string.name_pseudo_pro))
        }
    }

inline val AndroidViewModel.application
    get() = getApplication<Application>()

inline fun ViewGroup.inflate(id: Int, attachToRoot: Boolean = false): View =
        LayoutInflater.from(context).inflate(id, this, attachToRoot)

fun Context.withAttrs(vararg attrs: Int, block: TypedArray.() -> Unit) =
        withStyledAttributes(0, attrs, block)

inline operator fun File.invoke(sub: String) = File(this, sub)

inline fun Context.getColorCompat(@ColorRes id: Int) = ContextCompat.getColor(this, id)

fun AlertDialog.showWithLifecycle(fragment: Fragment) {
    val observer = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        @Suppress("unused")
        fun paused() {
            dismiss()
        }
    }
    setOnDismissListener { fragment.lifecycle.removeObserver(observer) }
    show()
    fragment.lifecycle.addObserver(observer)
}

var cachedWallpaper: Bitmap? by SoftReferenceDelegate()

fun Activity.applyCustomBackground() {
    when (prefs.getString(Common.BACKGROUND, "")) {
        "color" -> {
            window.setBackgroundDrawable(ColorDrawable(prefs.getInt(Common.BACKGROUND_COLOR, R.color.accent)))
        }
        "custom" -> try {
            val width = resources.displayMetrics.widthPixels
            val height = resources.displayMetrics.heightPixels
            val bitmap = cachedWallpaper ?: ThumbnailUtils.extractThumbnail(
                    BitmapFactory.decodeStream(openFileInput("background")),
                    width, height, OPTIONS_RECYCLE_INPUT)
                    .apply {
                        cachedWallpaper = this
                    }
            window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
        } catch (e: IOException) {
            Toast.makeText(this, "Error loading background image, IOException.", Toast.LENGTH_LONG).show()
        } catch (e: OutOfMemoryError) {
            Toast.makeText(this, "Error loading background image, is it to big?", Toast.LENGTH_LONG).show()
        }
        else -> window.setBackgroundDrawableResource(android.R.color.transparent) // system
    }
}

fun Context.getApplicationIcon(packageName: String): Drawable = try {
    packageManager.getApplicationIcon(packageName)
} catch (e: PackageManager.NameNotFoundException) {
    ContextCompat.getDrawable(this, R.mipmap.ic_launcher) ?: ColorDrawable(TRANSPARENT)
}

fun View.showIme() {
    val imm = context.getSystemService<InputMethodManager>()
    imm?.showSoftInput(this, 0)
}

fun Activity.hideIme() {
    val imm = getSystemService<InputMethodManager>()
    Handler().postDelayed(20) {
        imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
}

inline fun TextView.clearText() {
    text = ""
}

/**
 * From [http://stackoverflow.com/a/11978976]. Thanks very much!
 */
fun String.toSha256(): String? = try {
    val digest = MessageDigest.getInstance("SHA-256")
    var bytes = toByteArray(StandardCharsets.UTF_8)
    digest.update(bytes, 0, bytes.size)
    bytes = digest.digest()
    bytes.toHexString()
} catch (e: NoSuchAlgorithmException) {
    e.printStackTrace()
    null
}

private val hexArray = "0123456789ABCDEF".toCharArray()

private fun ByteArray.toHexString(): String {
    val hexChars = CharArray(size * 2)
    for (j in indices) {
        val v = this[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}

object KUtil {
    @JvmStatic
    fun getLauncherPackages(pm: PackageManager): List<String> {
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
                .mapTo(ArrayList<String>()) { it.activityInfo.packageName }.apply { remove(SETTINGS_PACKAGE_NAME) }
    }

    @JvmStatic
    fun getPatternCode(app: Int): Int = if (app == -1) PATTERN_CODE else (PATTERN_CODE_APP.toString() + app.toString()).toInt()
}