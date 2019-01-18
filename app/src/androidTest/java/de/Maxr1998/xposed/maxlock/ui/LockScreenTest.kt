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

package de.Maxr1998.xposed.maxlock.ui


import android.content.Intent
import androidx.core.content.edit
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.zhuinden.espressohelper.*
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.Util
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LockScreenTest {

    private val testPackageName = "de.Maxr1998.xposed.maxlock.test_package_name"

    @Rule
    @JvmField
    var rule = ActivityTestRule(LockActivity::class.java, false, false)

    private fun setupAndLaunchActivity(lockingType: String, password: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        MLPreferences.getPreferences(context).edit {
            putString(Common.LOCKING_TYPE, Common.PREF_VALUE_PIN)
        }
        MLPreferences.getPreferencesKeysPerApp(context).edit {
            putString(testPackageName, lockingType)
            putString(testPackageName + Common.APP_KEY_PREFERENCE, Util.shaHash(password))
        }
        rule.launchActivity(Intent().putExtra(Common.INTENT_EXTRA_APP_NAMES, arrayOf(testPackageName, "")))

        Thread.sleep(1000)
    }

    @Test
    fun passwordTest() {
        val password = "thisisatest"
        setupAndLaunchActivity(Common.PREF_VALUE_PASSWORD, password)
        R.id.input_view.apply {
            checkIsDisplayed()
            // Wrong input
            performTypeText("foo").performPressImeActionButton()
            Thread.sleep(50)
            checkHasEmptyText()
            // Correct input
            performTypeText(password).performPressImeActionButton()
            Thread.sleep(100)
        }
        assert(rule.activity.isFinishing)
    }

    @Test
    fun pinTest() {
        val password = StringBuilder()
        for (i in 1..4)
            password.append((Math.round(Math.random() * 3).toInt() + 1).toString())
        setupAndLaunchActivity(Common.PREF_VALUE_PIN, password.toString())
        for (i in password)
            onView(ViewMatchers.withText(i.toString())).performClick()
        assert(rule.activity.isFinishing)
    }

    @Test
    fun knockCodeTest() {
        val password = StringBuilder()
        for (i in 1..5)
            password.append((Math.round(Math.random() * 3).toInt() + 1).toString())
        setupAndLaunchActivity(Common.PREF_VALUE_KNOCK_CODE, password.toString())
        for (i in password) {
            R.id.container.performViewAction(clickAtPosition(100 + if (i.toString().toInt().isEven()) 500 else 0, 100 + if (i > '2') 500 else 0))
        }
    }

    private fun clickAtPosition(x: Int, y: Int): ViewAction = GeneralClickAction(
            Tap.SINGLE,
            CoordinatesProvider { view ->
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)
                floatArrayOf((screenPos[0] + x).toFloat(), (screenPos[1] + y).toFloat())
            },
            Press.FINGER,
            0,
            0
    )

    private fun Int.isEven() = this == (this / 2 * 2)
}