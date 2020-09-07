/*
 * This file is part of eduVPN.
 *
 * eduVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * eduVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package nl.eduvpn.app.ui_test

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import nl.eduvpn.app.BaseRobot
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.waitUntilGone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests if the connection log opens correctly. This is an activity of the ics-openvpn library,
 * so it could break between a version upgrade.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionLogTest {

    companion object {
        private val TAG = ConnectionLogTest::class.java.name
    }

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testOpenConnectionLog() {
        // Wait for list to load
        try {
            onView(withText("Fetching organizations...")).perform(waitUntilGone(2_000L))
        } catch (ex: Exception) {
            Log.i(TAG, "Couldn't find loading popup")
        }

        BaseRobot().waitForView(withText("Institute Access"), waitMillis = 2_000).check(matches(isDisplayed()))
        // Open the connection log from settings
        onView(ViewMatchers.withId(R.id.settingsButton)).perform(ViewActions.click())
        onView(ViewMatchers.withId(R.id.settings_scroller)).perform(swipeUp())
        onView(withText("View Log")).perform(ViewActions.click())
        onView(withText("OpenVPN Log")).check(matches(isDisplayed()))
    }

}