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

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        Instance instance = new Instance("http://example.com", "Example", "http://example.com/image.jpg", AuthorizationType.Distributed, true);
        _preferencesService.setCurrentInstance(instance);
        Instance retrievedInstance = _preferencesService.getCurrentInstance();
        assertNotNull(retrievedInstance);
        assertEquals(instance.getDisplayName(), retrievedInstance.getDisplayName());
        assertEquals(instance.getLogoUri(), retrievedInstance.getLogoUri());
        assertEquals(instance.getBaseURI(), retrievedInstance.getBaseURI());
        assertEquals(instance.isCustom(), retrievedInstance.isCustom());
    }

    @Test
    public void testDiscoveredAPISave() {
        DiscoveredAPI discoveredAPI = new DiscoveredAPI("http://example.com/", "http://example.com/auth_endpoint", "http://example.com/token_endpoint");
        _preferencesService.setCurrentDiscoveredAPI(discoveredAPI);
        DiscoveredAPI retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI();
        //noinspection ConstantConditions
        assertEquals(discoveredAPI.getAuthorizationEndpoint(), retrievedDiscoveredAPI.getAuthorizationEndpoint());
        assertEquals(discoveredAPI.getApiBaseUri(), retrievedDiscoveredAPI.getApiBaseUri());
        assertEquals(discoveredAPI.getTokenEndpoint(), retrievedDiscoveredAPI.getTokenEndpoint());
    }

    @Test
    public void testMigration() throws SerializerService.UnknownFormatException {
        // We only test a few properties
        DiscoveredAPI discoveredAPI = new DiscoveredAPI("http://example.com/", "http://example.com/auth_endpoint", "http://example.com/token_endpoint");
        Instance instance = new Instance("base_uri", "display_name", "logo_uri", AuthorizationType.Distributed, false);
        SharedPreferences.Editor editor = _oldPreferences.edit();

        SerializerService serializerService = new SerializerService();

        editor.putString(PreferencesService.KEY_INSTANCE, serializerService.serializeInstance(instance).toString());
        editor.putString(PreferencesService.KEY_DISCOVERED_API, serializerService.serializeDiscoveredAPI(discoveredAPI).toString());
        editor.commit();

        _preferencesService._getSharedPreferences().edit().clear().commit();
        _preferencesService._migrateIfNeeded(_preferencesService._getSharedPreferences(), _oldPreferences);

        Instance instanceResult = _preferencesService.getCurrentInstance();
        DiscoveredAPI retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI();

        //noinspection ConstantConditions
        assertEquals(discoveredAPI.getAuthorizationEndpoint(), retrievedDiscoveredAPI.getAuthorizationEndpoint());
        assertEquals(discoveredAPI.getApiBaseUri(), retrievedDiscoveredAPI.getApiBaseUri());
        assertEquals(discoveredAPI.getTokenEndpoint(), retrievedDiscoveredAPI.getTokenEndpoint());

        assertEquals(instanceResult.getBaseURI(), instanceResult.getBaseURI());
        assertEquals(instanceResult.getDisplayName(), instanceResult.getDisplayName());
        assertEquals(instanceResult.getAuthorizationType(), instanceResult.getAuthorizationType());
    }

}
