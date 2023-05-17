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
package nl.eduvpn.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import nl.eduvpn.app.entity.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the preferences service.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PreferencesServiceTest {

    private lateinit var _preferencesService: PreferencesService

    @Before
    fun before() {
        val serializerService = SerializerService()
        val context = ApplicationProvider.getApplicationContext<Context>()
        _preferencesService = PreferencesService(
            context,
            serializerService
        )
    }

    @Test
    fun testInstanceSave() {
        val instance = Instance(
            "http://example.com",
            TranslatableString("Example"),
            TranslatableString("konijn"),
            "http://example.com/image.jpg",
            AuthorizationType.Distributed,
            "HU",
            true,
            "https://example.com/template",
            ArrayList()
        )
        _preferencesService.setCurrentInstance(instance)
        val retrievedInstance = _preferencesService.getCurrentInstance()
        Assert.assertNotNull(retrievedInstance)
        Assert.assertEquals(instance.displayName, retrievedInstance!!.displayName)
        Assert.assertEquals(instance.logoUri, retrievedInstance.logoUri)
        Assert.assertEquals(instance.baseURI, retrievedInstance.baseURI)
        Assert.assertEquals(instance.isCustom, retrievedInstance.isCustom)
        Assert.assertEquals(instance.countryCode, retrievedInstance.countryCode)
        Assert.assertEquals(instance.supportContact, retrievedInstance.supportContact)
        Assert.assertEquals(
            instance.authenticationUrlTemplate,
            retrievedInstance.authenticationUrlTemplate
        )
    }

    @Test
    fun testDiscoveredAPISave() {
        val discoveredAPIV3 = DiscoveredAPIV3(
            "http://example.com/",
            "http://example.com/auth_endpoint",
            "http://example.com/token_endpoint"
        )
        _preferencesService.setCurrentDiscoveredAPI(discoveredAPIV3)
        val retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI()
        Assert.assertEquals(
            discoveredAPIV3.authorizationEndpoint,
            retrievedDiscoveredAPI!!.authorizationEndpoint
        )
        Assert.assertEquals(
            discoveredAPIV3.apiEndpoint,
            retrievedDiscoveredAPI.toDiscoveredAPIs().v3!!.apiEndpoint
        )
        Assert.assertEquals(discoveredAPIV3.tokenEndpoint, retrievedDiscoveredAPI.tokenEndpoint)
    }

    @Test
    fun testLastKnownOrganizationListVersionSave() {
        val version = 121_323L
        _preferencesService.setLastKnownOrganizationListVersion(version)
        val retrievedVersion = _preferencesService.getLastKnownOrganizationListVersion()
        Assert.assertEquals(version, retrievedVersion)
    }

    @Test
    fun testLastKnownServerListVersionSave() {
        val version = 8_982_398L
        _preferencesService.setLastKnownServerListVersion(version)
        val retrievedVersion = _preferencesService.getLastKnownServerListVersion()
        Assert.assertEquals(version, retrievedVersion)
    }
}
