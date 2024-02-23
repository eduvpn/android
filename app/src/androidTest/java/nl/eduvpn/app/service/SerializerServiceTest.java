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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Organization;
import nl.eduvpn.app.entity.OrganizationList;
import nl.eduvpn.app.entity.Settings;
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
    public void testAppSettingsSerialization() throws SerializerService.UnknownFormatException {
        Settings settings = new Settings(true, true);
        JSONObject jsonObject = _serializerService.serializeAppSettings(settings);
        Settings deserializedSettings = _serializerService.deserializeAppSettings(jsonObject);
        assertEquals(settings.preferTcp(), deserializedSettings.preferTcp());
        assertEquals(settings.useCustomTabs(), deserializedSettings.useCustomTabs());
        settings = new Settings(false, false);
        jsonObject = _serializerService.serializeAppSettings(settings);
        deserializedSettings = _serializerService.deserializeAppSettings(jsonObject);
        assertEquals(settings.preferTcp(), deserializedSettings.preferTcp());
        assertEquals(settings.useCustomTabs(), deserializedSettings.useCustomTabs());
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

    @Test
    public void testOrganizationListSerialization() throws SerializerService.UnknownFormatException {
        Map<String, String> keywordsMap = new HashMap<>();
        keywordsMap.put("en", "english keyword");
        keywordsMap.put("de", "german keyword");
        keywordsMap.put("nl", "dutch keyword");
        Organization organization1 = new Organization("orgid-1", new TranslatableString("display name - 1"), new TranslatableString(keywordsMap), "https://server.info/url");
        Organization organization2 = new Organization("orgid-2", new TranslatableString("display name - 2"), new TranslatableString("notthesamekeyword"), "https://server.info2/url");
        Organization organization3 = new Organization("orgid-3", new TranslatableString("display name - 3"), new TranslatableString(), "http://server.info/url3");
        List<Organization> organizations = Arrays.asList(organization1, organization2, organization3);
        OrganizationList organizationList = new OrganizationList(12345L, organizations);
        JSONObject serializedOrganizationList = _serializerService.serializeOrganizationList(organizationList);
        OrganizationList deserializedOrganizationList = _serializerService.deserializeOrganizationList(serializedOrganizationList);
        for (int i = 0; i < organizations.size(); ++i) {
            assertEquals(organizations.get(i).getDisplayName(), deserializedOrganizationList.getOrganizationList().get(i).getDisplayName());
            assertEquals(organizations.get(i).getKeywordList(), deserializedOrganizationList.getOrganizationList().get(i).getKeywordList());
            assertEquals(organizations.get(i).getOrgId(), deserializedOrganizationList.getOrganizationList().get(i).getOrgId());
            assertEquals(organizations.get(i).getSecureInternetHome(), deserializedOrganizationList.getOrganizationList().get(i).getSecureInternetHome());
            assertEquals(organizationList.getVersion(), deserializedOrganizationList.getVersion());
        }
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
