package de.Maxr1998.xposed.maxlock.util

import android.content.Context
import android.content.SharedPreferences

inline val Context.prefs: SharedPreferences
    get() = MLPreferences.getPreferences(this)
inline val Context.prefsApps: SharedPreferences
    get() = MLPreferences.getPrefsApps(this)