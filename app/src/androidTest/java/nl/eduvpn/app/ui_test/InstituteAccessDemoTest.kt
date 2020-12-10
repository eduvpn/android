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
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import nl.eduvpn.app.BaseRobot
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.waitUntilGone
import org.hamcrest.CoreMatchers
import org.hamcrest.core.AllOf
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
class InstituteAccessDemoTest {

    companion object {
        private val TAG = InstituteAccessDemoTest::class.java.name

        private const val DEMO_ORGANIZATION_NAME = "Demo"

        private const val DEMO_USER = "daniel+demo@egeniq.com"
        private const val DEMO_PASSWORD = "AndroidTest123"
    }

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    private val isLetsConnect = BuildConfig.FLAVOR == "home"

    @Test
    fun testAddDemoServer() {
        if (isLetsConnect) {
            fail("This test only works on the EduVPN app!")
            return
        }
        // Wait for list to load
        try {
            onView(withText("Fetching organizations...")).perform(waitUntilGone(2_000L))
        } catch (ex: Exception) {
            Log.i(TAG, "Couldn't find loading popup")
        }
        BaseRobot().waitForView(withText("Institute Access"), waitMillis = 2_000).check(matches(isDisplayed()))
        onView(withHint("Search for your organization...")).perform(typeText(DEMO_ORGANIZATION_NAME), closeSoftKeyboard())
        BaseRobot().waitForView(
                AllOf.allOf(withText(DEMO_ORGANIZATION_NAME), ViewMatchers.withClassName(CoreMatchers.containsString("TextView")))
        ).perform(click())
        // Switch over to UI Automator now, to control the browser
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val selector = UiSelector()
        // Wait for the browser to open and load
        Thread.sleep(2_000L)
        try {
            // Chrome asks at first launch to accept data usage
            val acceptButton = device.findObject(UiSelector().className("android.widget.Button").text("Accept & continue"))
            acceptButton.click()
        } catch (ex: UiObjectNotFoundException) {
            Log.w(TAG, "No Chrome accept window shown, continuing", ex)
        }
        try {
            // Do not send all our web traffic to Google
            val liteModeToggle = device.findObject(UiSelector().className("android.widget.Switch"))
            if(liteModeToggle.isChecked) {
                liteModeToggle.click()
            }
            val nextButton = device.findObject(UiSelector().className("android.widget.Button").text("Next"))
            nextButton.click()
        } catch (ex: UiObjectNotFoundException) {
            Log.w(TAG, "No lite mode window shown, continuing", ex)
        }
        try {
            // Now it wants us to Sign in...
            val noThanksButton = device.findObject(UiSelector().text("No thanks"))
            noThanksButton.click()
        } catch (ex: UiObjectNotFoundException) {
            Log.w(TAG, "No request for sign in, continung", ex)
        }
        try {
            // Select eduID from the list
            val eduIDButton = device.findObject(selector.text("eduID (NL)"))
            eduIDButton.click()
            try {
                eduIDButton.click() // Sometimes doesn't work
            } catch (ex: Exception) {
                // Not handled
            }
            Thread.sleep(1_000L)
            // We can't find objects based on hints here, so we do it on layout order instead.
            Log.v(TAG, "Entering email address.")
            val userName = device.findObject(selector.className("android.widget.EditText").instance(0))
            userName.click()
            userName.text = DEMO_USER
            device.pressBack()
            try {
                Log.v(TAG, "Clicking 'Type a password' link")
                val typePasswordLink = device.findObject(selector.text("Type a password."))
                typePasswordLink.click()
                Thread.sleep(500L)
            } catch (ex: Exception) {
                // Type a password preference is sometimes remembered.
            }
            Log.v(TAG, "Entering password.")
            val password = device.findObject(selector.className("android.widget.EditText").instance(1))
            password.click()
            password.text = DEMO_PASSWORD
            device.pressBack()
            Log.v(TAG, "Logging in...")
            val loginButton = device.findObject(selector.text("Login"))
            loginButton.click()
        } catch (ex: UiObjectNotFoundException) {
            // Perhaps we are still logged in. In this case, we get to the approve page, so continue
            Log.w(TAG, "Object not found, user is perhaps logged in already.", ex)
        }
        try {
            // Chrome sometimes asks to remember the password. We don't want to
            Log.v(TAG, "Hiding the 'remember password' dialog.")
            val hideRememberPasswordDialogButton = device.findObject(selector.resourceId("com.android.chrome:id/infobar_close_button"))
            hideRememberPasswordDialogButton.click()
        } catch (ex: Exception) {
            Log.w(TAG, "Could not hide remember password dialog. Probably not a Chrome browser", ex)
        }
        // Now we have to approve the app, but first we need to scroll to the bottom.
        Log.v(TAG, "Scrolling down.")
        val webView = UiScrollable(selector.className("android.webkit.WebView").scrollable(true))
        webView.scrollToEnd(2)
        Log.v(TAG, "Approving VPN app.")
        val approveButton = device.findObject(selector.text("Approve"))
        approveButton.click()
        try {
            approveButton.click() // Sometimes it doesn't work :)
        } catch (ex: Exception) {
            // Unhandled
        }
        BaseRobot().waitForView(withText("Demo")).check(matches(isDisplayed()))
    }

}