package nl.eduvpn.app.ui_test

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import nl.eduvpn.app.utils.Log

abstract class BrowserTest {

    companion object {
        private val TAG = BrowserTest::class.java.name
    }
    fun prepareBrowser() {
        // Switch over to UI Automator now, to control the browser
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Wait for the browser to open and load
        Thread.sleep(2_000L)
        try {
            // Newer Chrome versions ask if you want to log in
            val acceptButton = device.findObject(UiSelector().text("Use without an account"))
            acceptButton.click()
        } catch (ex: UiObjectNotFoundException) {
            Log.w(TAG, "No Chrome user account shown, continuing", ex)
        }
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
    }
}