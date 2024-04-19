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

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.*
import nl.eduvpn.app.BaseRobot
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.waitUntilGone
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.core.AllOf.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests if the flow of connecting to a VPN provider fully works.
 * This test only works both on eduVPN and LetsConnect.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectVpnTest : BrowserTest() {

    companion object {
        private val TAG = ConnectVpnTest::class.java.name

        private const val TEST_SERVER_URL = "vpntest.spoor.nu"

        private const val TEST_SERVER_USERNAME = "google"
        private const val TEST_SERVER_PASSWORD = "auto-test"
    }

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    private val isLetsConnect = BuildConfig.FLAVOR == "home"

    @Test
    fun testVpnConnectFlow() {
        if (isLetsConnect) {
            onView(withHint("Enter server address here")).perform(typeText(TEST_SERVER_URL))
            closeSoftKeyboard()
            onView(withText("ADD SERVER")).perform(click())
        } else {
            // Wait for list to load
            try {
                onView(withText("Fetching organizations...")).perform(waitUntilGone(5_000L))
            } catch (ex: Exception) {
                Log.i(TAG, "Couldn't find loading popup")
            }
            BaseRobot().waitForView(withText("Institute Access"), waitMillis = 2_000).check(matches(isDisplayed()))
            onView(withHint("Search for your organization...")).perform(typeText(TEST_SERVER_URL))
            closeSoftKeyboard()
            BaseRobot().waitForView(
                    allOf(withText(TEST_SERVER_URL), withClassName(containsString("TextView")))
            ).perform(click())
        }
        prepareBrowser()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            // We can't find objects based on hints here, so we do it on layout order instead.
            Log.v(TAG, "Entering username.")
            val userName = device.findObject(UiSelector().className("android.widget.EditText").instance(0))
            userName.click()
            userName.setText(TEST_SERVER_USERNAME)
            Log.v(TAG, "Entering password.")
            var password: UiObject?
            try {
                password = device.findObject(UiSelector().className("android.widget.EditText").instance(1))
                password.click()
            } catch (ex: Exception) {
                Log.v(TAG, "Scrolling down to see password input")
                val webView = UiScrollable(UiSelector().className("android.webkit.WebView").scrollable(true))
                webView.flingToEnd(1)
                Log.v(TAG, "Entering password again.")
                password = device.findObject(UiSelector().className("android.widget.EditText").instance(1))
                password.click()
            }
            password?.setText(TEST_SERVER_PASSWORD)
            Log.v(TAG, "Hiding keyboard.")
            device.pressBack() // Closes the keyboard
            Log.v(TAG, "Signing in.")
            val signInButton = device.findObject(UiSelector().className("android.widget.Button").text("Sign In"))
            signInButton.click()
        } catch (ex: UiObjectNotFoundException) {
            // Perhaps we are still logged in. In this case, we get to the approve page, so continue
            Log.w(TAG, "Object not found, user is perhaps logged in already.", ex)
        }
        try {
            // Chrome sometimes asks to remember the password. We don't want to
            Log.v(TAG, "Hiding the 'remember password' dialog.")
            val hideRememberPasswordDialogButton = device.findObject(UiSelector().resourceId("com.android.chrome:id/infobar_close_button"))
            hideRememberPasswordDialogButton.click()
        } catch (ex: Exception) {
            Log.w(TAG, "Could not hide remember password dialog. Probably not a Chrome browser", ex)
        }
        // Now we have to approve the app, but first we need to scroll to the bottom.
        Log.v(TAG, "Scrolling down.")
        val webView = UiScrollable(UiSelector().className("android.webkit.WebView").scrollable(true))
        webView.scrollToEnd(2)
        Log.v(TAG, "Approving VPN app.")
        val approveButton = device.findObject(UiSelector().text("Approve"))
        approveButton.click()
        // Wait for the app to display the server in its list
        BaseRobot().waitForView(withText("vpntest.spoor.nu")).perform(click())
        try {
            // We need the UI Automator again, because VPN accept permission is a system dialog
            val isDialogShown = Until.findObject(By.text("Connection request"))
            device.wait(isDialogShown, 3_000L)
            val okButton = device.findObject(UiSelector().className("android.widget.Button").text("OK"))
            okButton.click()
        } catch (ex: Exception) {
            Log.w(TAG, "No connection request shown, probably accepted already once.", ex)
        }
        BaseRobot().waitForView(withText("Connected")).check(matches(isDisplayed()))
    }

}
