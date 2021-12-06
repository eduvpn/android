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
package nl.eduvpn.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import nl.eduvpn.app.service.SecurityService.Companion.loadMinisignPublicKeys
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@LargeTest
class SecurityServiceTest {

    companion object {
        // If you wish to create more unit tests, these are the parameters:
        private const val UNIT_TEST_PRIVATE_KEY =
            "untrusted comment: minisign encrypted secret key\n" +
                    "RWRTY0Iyq5pf3lO+R4dtzZDgzvhE6jFtvUd9A1XIz2FN9ZfhhjkAAAACAAAAAAAAAEAAAAAADmmEh0zDiH5308dE01SbOhbIJ6sxxX+1qecOjIBvJuQ0W4uxMk37j3qNCyHD8HhOHsQqa1Jty6ztnnrKT1itKQR2uWPW4kNcnGzQYoLkz/rzESMI2jZE98W7LvV8LQrGv9AMTgOwOPY="
        private val UNIT_TEST_PUBLIC_KEYS =
            arrayOf("RWQBThy5Bd7KteZuDmjwUq/6E8IIoOETi85bBcIHz0dj1VokayIb/FYb")
    }

    private lateinit var securityService: SecurityService

    @Before
    fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        securityService = SecurityService(context)
        loadMinisignPublicKeys(UNIT_TEST_PUBLIC_KEYS)
    }

    @Test
    fun testMinisignGetSecondLineSuccess() {
        val input = "1234\n5678"
        val output = securityService.getSecondLine(input)
        Assert.assertEquals("5678", output)
    }

    @Test(expected = IOException::class)
    fun testMinisignGetSecondLineError() {
        val input = "12345678"
        securityService.getSecondLine(input)
    }

    @Test
    fun testMinisignVerifySuccess() {
        // Generated these on my local machine
        val toVerify = "test text signed by eduvpn dev"
        val signature = """
            untrusted comment: signature from minisign secret key
            RWQBThy5Bd7KtZdpjhBiwppvZdTt9nc23OVuBQCcNJ6LT5MgIcA4wLxjgGIOMGEbaZVLxqrHNRMWQ3JSGRWn2CxE6UVF+QplMA4=
            trusted comment: timestamp:1584026575	file:test.txt
            tOgVBGUEo6HVEEz49P7thyDMZsSrtEHBrz60n/TYaOk4PBNdgXl46z9rG/k3Xul9ewzNeOWY/hv1E2EMEVldDg==
            """.trimIndent()
        Assert.assertTrue(securityService.verifyMinisign(toVerify, signature))
    }

    @Test(expected = IOException::class)
    fun testMinisignVerifyOneLineFail() {
        val toVerify = "test text signed by eduvpn dev"
        val signature = "this is of course the wrong signature"
        Assert.assertFalse(securityService.verifyMinisign(toVerify, signature))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMinisignVerifySignatureFormatException() {
        val toVerify = "test text signed by eduvpn dev"
        val signature =
            "this is of course the wrong signature\naW4gMiBsaW5lcyBzbyB0aGF0IGl0IHdpbGwgZW50ZXIgdGhlIHZlcmlmaWNhdGlvbiBwaGFzZQ=="
        Assert.assertFalse(securityService.verifyMinisign(toVerify, signature))
    }

    @Test
    fun testMinisignVerifySignatureFail() {
        val toVerify =
            "a completely different text which has the same signature as the success text"
        val signature = """
            untrusted comment: signature from minisign secret key
            RWQBThy5Bd7KtZdpjhBiwppvZdTt9nc23OVuBQCcNJ6LT5MgIcA4wLxjgGIOMGEbaZVLxqrHNRMWQ3JSGRWn2CxE6UVF+QplMA4=
            trusted comment: timestamp:1584026575	file:test.txt
            tOgVBGUEo6HVEEz49P7thyDMZsSrtEHBrz60n/TYaOk4PBNdgXl46z9rG/k3Xul9ewzNeOWY/hv1E2EMEVldDg==
            """.trimIndent()
        Assert.assertFalse(securityService.verifyMinisign(toVerify, signature))
    }
}
