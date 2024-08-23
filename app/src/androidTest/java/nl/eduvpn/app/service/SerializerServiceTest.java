/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.service;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.TranslatableString;

/**
 * Unit tests for the serializer service.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SerializerServiceTest {

    private SerializerService _serializerService;

    @Before
    public void before() {
        _serializerService = new SerializerService();
    }

    @Test
    public void testInstanceSerialization() throws SerializerService.UnknownFormatException {
        Instance instance = new Instance("baseUri", new TranslatableString("displayName"), new TranslatableString("konijn"), "logoUri", AuthorizationType.Distributed, "HU", true, null, Arrays.asList("mailto:user@test.example.com", "tel:+0011223344659898"));
        String serializedInstance = _serializerService.serializeInstance(instance);
        Instance deserializedInstance = _serializerService.deserializeInstance(serializedInstance);
        assertEquals(instance.getDisplayName(), deserializedInstance.getDisplayName());
        assertEquals(instance.getBaseURI(), deserializedInstance.getBaseURI());
        assertEquals(instance.getLogoUri(), deserializedInstance.getLogoUri());
        assertEquals(instance.getAuthorizationType(), deserializedInstance.getAuthorizationType());
        assertEquals(instance.isCustom(), deserializedInstance.isCustom());
        assertEquals(instance.getCountryCode(), deserializedInstance.getCountryCode());
        assertEquals(instance.getSupportContact(), deserializedInstance.getSupportContact());
    }

    /**
     * Removes the milliseconds from a date. Required because the parser does not care about milliseconds.
     *
     * @param input The date to remove the milliseconds from.
     * @return The date without the milliseconds.
     */
    public static Date _norm(Date input) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(input);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

}
