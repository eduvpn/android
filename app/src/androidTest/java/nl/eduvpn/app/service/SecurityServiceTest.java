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

package nl.eduvpn.app.service;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SecurityServiceTest {

    // If you wish to create more unit tests, these are the parameters:
    @SuppressWarnings({ "unused", "RedundantSuppression" })
    private static final String UNIT_TEST_PRIVATE_KEY = "untrusted comment: minisign encrypted secret key\n" +
            "RWRTY0Iyq5pf3lO+R4dtzZDgzvhE6jFtvUd9A1XIz2FN9ZfhhjkAAAACAAAAAAAAAEAAAAAADmmEh0zDiH5308dE01SbOhbIJ6sxxX+1qecOjIBvJuQ0W4uxMk37j3qNCyHD8HhOHsQqa1Jty6ztnnrKT1itKQR2uWPW4kNcnGzQYoLkz/rzESMI2jZE98W7LvV8LQrGv9AMTgOwOPY=";
    private static final String[] UNIT_TEST_PUBLIC_KEYS = new String[] { "RWQBThy5Bd7KteZuDmjwUq/6E8IIoOETi85bBcIHz0dj1VokayIb/FYb" };

    private SecurityService _securityService;

    @Before
    public void before() {
        Context context = ApplicationProvider.getApplicationContext();
        _securityService = new SecurityService(context);
        SecurityService.loadMinisignPublicKeys(UNIT_TEST_PUBLIC_KEYS);
    }

    @Test
    public void testMinisignGetSecondLineSuccess() throws IOException {
        String input = "1234\n5678";
        String output = _securityService.getSecondLine(input);
        assertEquals("5678", output);
    }

    @Test(expected = IOException.class)
    public void testMinisignGetSecondLineError() throws IOException {
        String input = "12345678";
        _securityService.getSecondLine(input);
    }

    @Test
    public void testMinisignVerifySuccess() throws Exception {
        // Generated these on my local machine
        String toVerify = "test text signed by eduvpn dev";
        String signature = "untrusted comment: signature from minisign secret key\n" +
                "RWQBThy5Bd7KtZdpjhBiwppvZdTt9nc23OVuBQCcNJ6LT5MgIcA4wLxjgGIOMGEbaZVLxqrHNRMWQ3JSGRWn2CxE6UVF+QplMA4=\n" +
                "trusted comment: timestamp:1584026575\tfile:test.txt\n" +
                "tOgVBGUEo6HVEEz49P7thyDMZsSrtEHBrz60n/TYaOk4PBNdgXl46z9rG/k3Xul9ewzNeOWY/hv1E2EMEVldDg==";
        assertTrue(_securityService.verifyMinisign(toVerify, signature));
    }

    @Test(expected = IOException.class)
    public void testMinisignVerifyOneLineFail() throws Exception {
        String toVerify = "test text signed by eduvpn dev";
        String signature = "this is of course the wrong signature";
        assertFalse(_securityService.verifyMinisign(toVerify, signature));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinisignVerifySignatureFormatException() throws Exception {
        String toVerify = "test text signed by eduvpn dev";
        String signature = "this is of course the wrong signature\naW4gMiBsaW5lcyBzbyB0aGF0IGl0IHdpbGwgZW50ZXIgdGhlIHZlcmlmaWNhdGlvbiBwaGFzZQ==";
        assertFalse(_securityService.verifyMinisign(toVerify, signature));
    }

    @Test
    public void testMinisignVerifySignatureFail() throws Exception {
        String toVerify = "a completely different text which has the same signature as the success text";
        String signature = "untrusted comment: signature from minisign secret key\n" +
                "RWQBThy5Bd7KtZdpjhBiwppvZdTt9nc23OVuBQCcNJ6LT5MgIcA4wLxjgGIOMGEbaZVLxqrHNRMWQ3JSGRWn2CxE6UVF+QplMA4=\n" +
                "trusted comment: timestamp:1584026575\tfile:test.txt\n" +
                "tOgVBGUEo6HVEEz49P7thyDMZsSrtEHBrz60n/TYaOk4PBNdgXl46z9rG/k3Xul9ewzNeOWY/hv1E2EMEVldDg==";
        assertFalse(_securityService.verifyMinisign(toVerify, signature));
    }


}