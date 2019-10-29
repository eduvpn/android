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

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Tests for the preferences service.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PreferencesServiceTest {

    private PreferencesService _preferencesService;

    @Before
    public void before() {
        SerializerService serializerService = new SerializerService();
        Context context = InstrumentationRegistry.getContext();
        _preferencesService = new PreferencesService(serializerService, new SecurityService(context).getSecurePreferences());
    }

    @Test
    public void testInstanceSave() {
        Instance instance = new Instance("http://example.com", "Example", "http://example.com/image.jpg", AuthorizationType.DISTRIBUTED, true);
        _preferencesService.currentInstance(instance);
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
        _preferencesService.storeCurrentDiscoveredAPI(discoveredAPI);
        DiscoveredAPI retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI();
        //noinspection ConstantConditions
        assertEquals(discoveredAPI.getAuthorizationEndpoint(), retrievedDiscoveredAPI.getAuthorizationEndpoint());
        assertEquals(discoveredAPI.getApiBaseUri(), retrievedDiscoveredAPI.getApiBaseUri());
        assertEquals(discoveredAPI.getTokenEndpoint(), retrievedDiscoveredAPI.getTokenEndpoint());}

}
