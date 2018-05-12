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


import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.swipeLeft
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
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
    fun mainTest() {
        Thread.sleep(1000)

        val prefsApps = MLPreferences.getPrefsApps(activityTestRule.activity)

        // Skip welcome
        val viewPager = onView(allOf(withId(R.id.mi_pager), isDisplayed()))
        viewPager.perform(swipeLeft())
        Thread.sleep(250)

        // Skip info
        val nextButton = onView(allOf(withId(R.id.mi_button_next), isDisplayed()))
        nextButton.perform(click())
        Thread.sleep(50)

        // Setup initial packages
        val items = listOf(
                Pair(R.id.first_start_app_package, R.string.fs_app_package_installer),
                Pair(R.id.first_start_app_settings, R.string.fs_app_settings),
                Pair(R.id.first_start_app_xposed, R.string.fs_app_xposed_installer)
        )
        for (i in items) {
            checkAndAssert(i)
        }
        checkAndAssert(items[1], false)
        assert(prefsApps.getBoolean(Common.XPOSED_PACKAGE_NAME, false), { "Xposed isn't locked!" })
        Thread.sleep(20)

        // Finish first start
        nextButton.perform(click())
    }

    private fun checkAndAssert(info: Pair<Int, Int>, checked: Boolean = true) {
        val checkbox = onView(allOf(withId(info.first), withText(info.second), isDisplayed()))
        checkbox.perform(click())
        checkbox.check { view, noViewFoundException ->
            assert(noViewFoundException == null)
            assertThat(view, if (checked) isChecked() else isNotChecked())
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