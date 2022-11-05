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
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import nl.eduvpn.app.BaseRobot
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.waitUntilGone
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests if searching for a provider based upon a secondary keyword is successful.
 * This test only works on eduVPN.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProviderSearchTest {

    companion object {
        private val TAG = ProviderSearchTest::class.java.name
    }

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    private val isLetsConnect = BuildConfig.FLAVOR == "home"

    @Test
    fun testProviderSearchRetry() {
        testProviderSearch(1)
    }

    private fun testProviderSearch(retryCount: Int) {
        if (isLetsConnect) {
            fail("This test only works on the EduVPN app!")
            return
        }
        // Wait for list to load
        try {
            onView(withText("Fetching organizations...")).perform(waitUntilGone(5_000L))
        } catch (ex: Exception) {
            Log.i(TAG, "Couldn't find loading popup")
        }

        BaseRobot().waitForView(withText("Institute Access"), waitMillis = 2_000)
            .check(matches(isDisplayed()))
        val searchView = onView(withHint("Search for your organization..."))
        searchView.perform(
            typeText("konijn"),
            closeSoftKeyboard()
        )
        try {
            // For some reason isDisplayed() does not work
            onView(withText("SURF BV")).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withText("SURF (New)")).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        } catch (e: Exception) {
            if (retryCount == 0) {
                throw e
            } else {
                searchView.perform(
                    clearText()
                )
                testProviderSearch(retryCount - 1)
            }
        }
    }
}
