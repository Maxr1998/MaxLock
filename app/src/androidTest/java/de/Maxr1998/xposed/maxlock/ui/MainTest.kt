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


import android.view.View
import android.view.ViewGroup
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.zhuinden.espressohelper.*
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainTest {

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(SettingsActivity::class.java)

    @Test
    fun setupTest() {
        Thread.sleep(1000)

        val prefsApps = MLPreferences.getPrefsApps(activityTestRule.activity)

        // Skip welcome
        R.id.mi_pager.performSwipeLeft()
        Thread.sleep(250)

        // Skip info
        R.id.mi_button_next.performClick()
        Thread.sleep(50)

        // Setup initial packages
        val items = listOf(
                Pair(R.id.first_start_app_package, R.string.fs_app_package_installer),
                Pair(R.id.first_start_app_settings, R.string.fs_app_settings),
                Pair(R.id.first_start_app_xposed, R.string.fs_app_xposed_installer)
        )
        for (i in items) {
            checkCheckbox(i)
        }
        checkCheckbox(items[1], false)
        assert(prefsApps.getBoolean(Common.XPOSED_PACKAGE_NAME, false), { "Xposed isn't locked!" })
        Thread.sleep(20)

        // Finish first start
        R.id.mi_button_next.performClick()
    }

    @Test
    fun mainTest() {

    }

    private fun checkCheckbox(info: Pair<Int, Int>, checked: Boolean = true) {
        info.first.apply {
            checkHasText(info.second)
            checkIsDisplayed()
            performSetCheckableChecked(checked)
        }
    }

    private fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}