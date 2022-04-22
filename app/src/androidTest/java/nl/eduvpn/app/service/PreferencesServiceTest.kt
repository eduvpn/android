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
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import nl.eduvpn.app.entity.*
import nl.eduvpn.app.service.SerializerService.UnknownFormatException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Tests for the preferences service.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PreferencesServiceTest {

    private lateinit var _preferencesService: PreferencesService
    private lateinit var _oldPreferences: SharedPreferences

    @Before
    fun before() {
        val serializerService = SerializerService()
        val context = ApplicationProvider.getApplicationContext<Context>()
        _preferencesService = PreferencesService(
            context,
            serializerService,
            @Suppress("DEPRECATION")
            SecurityService(context).securePreferences.also { _oldPreferences = it })
    }

    @Test
    fun testInstanceSave() {
        val instance = Instance(
            "http://example.com",
            TranslatableString("Example"),
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
        val discoveredAPIV2 = DiscoveredAPIV2(
            "http://example.com/",
            "http://example.com/auth_endpoint",
            "http://example.com/token_endpoint"
        )
        _preferencesService.setCurrentDiscoveredAPI(discoveredAPIV2)
        val retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI()
        Assert.assertEquals(
            discoveredAPIV2.authorizationEndpoint,
            retrievedDiscoveredAPI!!.authorizationEndpoint
        )
        Assert.assertEquals(
            discoveredAPIV2.apiBaseUri,
            retrievedDiscoveredAPI.toDiscoveredAPIs().v2!!.apiBaseUri
        )
        Assert.assertEquals(discoveredAPIV2.tokenEndpoint, retrievedDiscoveredAPI.tokenEndpoint)
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

    @Test
    @Throws(UnknownFormatException::class)
    fun testMigration() {
        // We only test a few properties
        val discoveredAPI = DiscoveredAPIV2(
            "http://example.com/",
            "http://example.com/auth_endpoint",
            "http://example.com/token_endpoint"
        )
        val discoveredAPIs = DiscoveredAPIs(discoveredAPI, null)
        val instance = Instance(
            "base_uri",
            TranslatableString("display_name"),
            "logo_uri",
            AuthorizationType.Distributed,
            "NL",
            false,
            "https://example.com/template",
            ArrayList()
        )
        val editor = _oldPreferences.edit()

        val serializerService = SerializerService()

        editor.putString(
            PreferencesService.KEY_INSTANCE, serializerService.serializeInstance(instance)
                .toString()
        )
        editor.putString(
            PreferencesService.KEY_DISCOVERED_API,
            serializerService.serializeDiscoveredAPIs(discoveredAPIs)
        )
        editor.commit()

        _preferencesService.getSharedPreferences().edit().clear().commit()
        _preferencesService.migrateIfNeeded(
            _preferencesService.getSharedPreferences(),
            _oldPreferences
        )

        val instanceResult = _preferencesService.getCurrentInstance()
        val retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI()

        Assert.assertEquals(
            discoveredAPI.authorizationEndpoint,
            retrievedDiscoveredAPI!!.authorizationEndpoint
        )
        Assert.assertEquals(
            discoveredAPI.apiBaseUri,
            retrievedDiscoveredAPI.toDiscoveredAPIs().v2!!.apiBaseUri
        )
        Assert.assertEquals(discoveredAPI.tokenEndpoint, retrievedDiscoveredAPI.tokenEndpoint)
        Assert.assertEquals(instanceResult!!.baseURI, instanceResult.baseURI)

        Assert.assertEquals(instanceResult.displayName, instanceResult.displayName)
        Assert.assertEquals(instanceResult.authorizationType, instanceResult.authorizationType)
        Assert.assertEquals(instanceResult.isCustom, instanceResult.isCustom)
        Assert.assertEquals(instanceResult.countryCode, instanceResult.countryCode)
        Assert.assertEquals(instanceResult.supportContact, instanceResult.supportContact)
        Assert.assertEquals(
            instanceResult.authenticationUrlTemplate,
            instanceResult.authenticationUrlTemplate
        )
    }
}
