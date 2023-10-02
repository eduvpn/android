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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SecurityServiceTest {

    companion object {
        // If you wish to create more unit tests, these are the parameters:
        // The private key is encrypted with an empty password.
        private const val UNIT_TEST_PRIVATE_KEY =
            "untrusted comment: minisign encrypted secret key\n" +
                    "RWRTY0IydHTrGGkyMdmKU2W/oGeSw1wMbEHJ7K66rGPtKdhxQPcAAAACAAAAAAAAAEAAAAAAr4HIfdsfVl+h+2tt6Wfh4tvkHn6q+Z0yuG32JtIJexV2CI5CCJN8V+QkUPPE9RstlyaVXiGhwJnp7bP9WoBosND5dfyHPFudALlO7cDpTEaBrBUouyaqu5Y0KtwbWQQkJDvhwzxR2zQ="
        private val UNIT_TEST_PUBLIC_KEYS =
            arrayOf("RWTVSfCL4u2OJhA5unM7ZFY5l+HOkyzOSGBL95mcPHUeqNpYWI3TzQcn")
    }

    private lateinit var securityService: SecurityService

    @Before
    fun before() {
        securityService = SecurityService()
        loadMinisignPublicKeys(UNIT_TEST_PUBLIC_KEYS)
    }

    @Test
    fun testMinisignGetSecondLineSuccess() {
        val input = "1234\n5678"
        val output = securityService.getSecondLine(input)
        Assert.assertEquals("5678", output)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMinisignGetSecondLineError() {
        val input = "12345678"
        securityService.getSecondLine(input)
    }

    @Test
    fun testMinisignVerifyLegacySuccess() {
        // Generated these on my local machine
        val toVerify = "test text signed by eduvpn dev\n".toByteArray()
        val signature = """
            untrusted comment: signature from minisign secret key
            RWTVSfCL4u2OJn0JIYGrDRabCed8+IhHIJYZkqJajfOBOmGpMKYr1fKX+cr9QBo3eufM4SEQfZu6jS19KKBLzmXIp9V4fNPlXwo=
            trusted comment: timestamp:1638884293	file:test.txt
            JN8QmOVtAFjkGB3bK9T6fYhaXshmEeZ0mQRNDfSPX4uD4GyUm6DIwzQb9DpyX0/ApAY1+w66FIQXd/+IpEBVAA==
            """.trimIndent()
        Assert.assertTrue(securityService.verifyMinisign(toVerify, signature))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMinisignVerifyOneLineFail() {
        val toVerify = "test text signed by eduvpn dev".toByteArray()
        val signature = "this is of course the wrong signature"
        Assert.assertFalse(securityService.verifyMinisign(toVerify, signature))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMinisignVerifySignatureFormatException() {
        val toVerify = "test text signed by eduvpn dev".toByteArray()
        val signature =
            "this is of course the wrong signature\naW4gMiBsaW5lcyBzbyB0aGF0IGl0IHdpbGwgZW50ZXIgdGhlIHZlcmlmaWNhdGlvbiBwaGFzZQ=="
        Assert.assertFalse(securityService.verifyMinisign(toVerify, signature))
    }

    @Test
    fun testMinisignVerifySignatureLegacyFail() {
        val toVerify =
            "a completely different text which has the same signature as the success text".toByteArray()
        val signature = """
            untrusted comment: signature from minisign secret key
            RWTVSfCL4u2OJn0JIYGrDRabCed8+IhHIJYZkqJajfOBOmGpMKYr1fKX+cr9QBo3eufM4SEQfZu6jS19KKBLzmXIp9V4fNPlXwo=
            trusted comment: timestamp:1638884293	file:test.txt
            JN8QmOVtAFjkGB3bK9T6fYhaXshmEeZ0mQRNDfSPX4uD4GyUm6DIwzQb9DpyX0/ApAY1+w66FIQXd/+IpEBVAA==
            """.trimIndent()
        Assert.assertFalse(securityService.verifyMinisign(toVerify, signature))
    }

    @Test
    fun testMinisignVerifyHashedSuccess() {
        val toVerify = "test text signed by eduvpn dev\n".toByteArray()
        val signature = """
            untrusted comment: signature from minisign secret key
            RUTVSfCL4u2OJsrz7ZONagn+2Z/KzzvDXSCOAV2qoKm8hC5xs6j8xymFVDmkG0kGgrAITLOBVFAA5lYjN+sCwIo8tAk2belgjQk=
            trusted comment: timestamp:1638884585	file:test.txt	hashed
            9+i1fAMvR6KcA5arwb6d8QyZbL260WgzT6dq/iD+VUxbWixNFmZuDVd0/LjvyEb8l9kI1+fM+e7Ci3YWnz3eAA==
            """.trimIndent()
        Assert.assertTrue(securityService.verifyMinisign(toVerify, signature))
    }

    fun testMinisignVerifyHashedFail() {
        val toVerify =
            "a completely different text which has the same signature as the success text".toByteArray()
        val signature = """
            untrusted comment: signature from minisign secret key
            RUTVSfCL4u2OJsrz7ZONagn+2Z/KzzvDXSCOAV2qoKm8hC5xs6j8xymFVDmkG0kGgrAITLOBVFAA5lYjN+sCwIo8tAk2belgjQk=
            trusted comment: timestamp:1638884585	file:test.txt	hashed
            9+i1fAMvR6KcA5arwb6d8QyZbL260WgzT6dq/iD+VUxbWixNFmZuDVd0/LjvyEb8l9kI1+fM+e7Ci3YWnz3eAA==
            """.trimIndent()
        Assert.assertTrue(securityService.verifyMinisign(toVerify, signature))
    }
}
