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
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.DiscoveredAPIV2;
import nl.eduvpn.app.entity.DiscoveredAPIs;
import nl.eduvpn.app.entity.Instance;

/**
 * Tests for the preferences service.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PreferencesServiceTest {

    private PreferencesService _preferencesService;
    private SharedPreferences _oldPreferences;

    @Before
    public void before() {
        SerializerService serializerService = new SerializerService();
        Context context = ApplicationProvider.getApplicationContext();
        _preferencesService = new PreferencesService(context, serializerService, _oldPreferences = new SecurityService(context).getSecurePreferences());
    }

    @Test
    public void testInstanceSave() {
        Instance instance = new Instance("http://example.com", "Example", "http://example.com/image.jpg", AuthorizationType.Distributed, "HU", true, "https://example.com/template", new ArrayList<>());
        _preferencesService.setCurrentInstance(instance);
        Instance retrievedInstance = _preferencesService.getCurrentInstance();
        assertNotNull(retrievedInstance);
        assertEquals(instance.getDisplayName(), retrievedInstance.getDisplayName());
        assertEquals(instance.getLogoUri(), retrievedInstance.getLogoUri());
        assertEquals(instance.getBaseURI(), retrievedInstance.getBaseURI());
        assertEquals(instance.isCustom(), retrievedInstance.isCustom());
        assertEquals(instance.getCountryCode(), retrievedInstance.getCountryCode());
        assertEquals(instance.getSupportContact(), retrievedInstance.getSupportContact());
        assertEquals(instance.getAuthenticationUrlTemplate(), retrievedInstance.getAuthenticationUrlTemplate());
    }

    @Test
    public void testDiscoveredAPISave() {
        DiscoveredAPIV2 discoveredAPIV2 = new DiscoveredAPIV2("http://example.com/", "http://example.com/auth_endpoint", "http://example.com/token_endpoint");
        _preferencesService.setCurrentDiscoveredAPI(discoveredAPIV2);
        DiscoveredAPI retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI();
        //noinspection ConstantConditions
        assertEquals(discoveredAPIV2.getAuthorizationEndpoint(), retrievedDiscoveredAPI.getAuthorizationEndpoint());
        assertEquals(discoveredAPIV2.getApiBaseUri(), retrievedDiscoveredAPI.toDiscoveredAPIs().getV2().getApiBaseUri());
        assertEquals(discoveredAPIV2.getTokenEndpoint(), retrievedDiscoveredAPI.getTokenEndpoint());
    }

    @Test
    public void testLastKnownOrganizationListVersionSave() {
        Long version = 121323L;
        _preferencesService.setLastKnownOrganizationListVersion(version);
        Long retrievedVersion = _preferencesService.getLastKnownOrganizationListVersion();
        assertEquals(version, retrievedVersion);
    }

    @Test
    public void testLastKnownServerListVersionSave() {
        Long version = 8982398L;
        _preferencesService.setLastKnownServerListVersion(version);
        Long retrievedVersion = _preferencesService.getLastKnownServerListVersion();
        assertEquals(version, retrievedVersion);
    }

    @Test
    public void testMigration() throws SerializerService.UnknownFormatException {
        // We only test a few properties
        DiscoveredAPIV2 discoveredAPI = new DiscoveredAPIV2("http://example.com/", "http://example.com/auth_endpoint", "http://example.com/token_endpoint");
        DiscoveredAPIs discoveredAPIs = new DiscoveredAPIs(discoveredAPI, null);
        Instance instance = new Instance("base_uri", "display_name", "logo_uri", AuthorizationType.Distributed, "NL", false, "https://example.com/template", new ArrayList<>());
        SharedPreferences.Editor editor = _oldPreferences.edit();

        SerializerService serializerService = new SerializerService();

        editor.putString(PreferencesService.KEY_INSTANCE, serializerService.serializeInstance(instance).toString());
        editor.putString(PreferencesService.KEY_DISCOVERED_API, serializerService.serializeDiscoveredAPIs(discoveredAPIs));
        editor.commit();

        _preferencesService._getSharedPreferences().edit().clear().commit();
        _preferencesService._migrateIfNeeded(_preferencesService._getSharedPreferences(), _oldPreferences);

        Instance instanceResult = _preferencesService.getCurrentInstance();
        DiscoveredAPI retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI();

        //noinspection ConstantConditions
        assertEquals(discoveredAPI.getAuthorizationEndpoint(), retrievedDiscoveredAPI.getAuthorizationEndpoint());
        assertEquals(discoveredAPI.getApiBaseUri(), retrievedDiscoveredAPI.toDiscoveredAPIs().getV2().getApiBaseUri());
        assertEquals(discoveredAPI.getTokenEndpoint(), retrievedDiscoveredAPI.getTokenEndpoint());

        assertEquals(instanceResult.getBaseURI(), instanceResult.getBaseURI());
        assertEquals(instanceResult.getDisplayName(), instanceResult.getDisplayName());
        assertEquals(instanceResult.getAuthorizationType(), instanceResult.getAuthorizationType());
        assertEquals(instanceResult.isCustom(), instanceResult.isCustom());
        assertEquals(instanceResult.getCountryCode(), instanceResult.getCountryCode());
        assertEquals(instanceResult.getSupportContact(), instanceResult.getSupportContact());
        assertEquals(instanceResult.getAuthenticationUrlTemplate(), instanceResult.getAuthenticationUrlTemplate());
    }

}
