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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.KeyPair;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for the history service.
 * Created by Daniel Zolnai on 2016-10-22.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HistoryServiceTest {

    private HistoryService _historyService;
    private static SharedPreferences _securePreferences;

    @Before
    @After
    public void clearPrefs() throws Exception {
        _reloadHistoryService(true);
    }

    @BeforeClass
    public static void setPreferences() {
        Context context = InstrumentationRegistry.getTargetContext();
        _securePreferences = new SecurityService(context).getSecurePreferences();
    }

    /**
     * Reloads the service, so we can test if the (de)serialization works as expected.
     *
     * @param clearHistory If the history should be cleared beforehand,
     */
    @SuppressLint({ "CommitPrefEdits", "ApplySharedPref" })
    private void _reloadHistoryService(boolean clearHistory) {
        SerializerService serializerService = new SerializerService();
        PreferencesService preferencesService = new PreferencesService(serializerService, _securePreferences);
        // Clean the shared preferences if needed
        if (clearHistory) {
            preferencesService._getSharedPreferences().edit().clear().commit();
        } else {
            // By doing a new commit, we make sure that all other pending transactions are being taken care of
            preferencesService._getSharedPreferences().edit().putInt("DUMMY_KEY", new Random().nextInt()).commit();
        }
        _historyService = new HistoryService(preferencesService);
    }

    @Test(timeout = 1000) // Could be a lot faster, but we use secure preferences, which encrypts and decrypts on-the-fly.
    public void testSerializationSpeed() {
        // We create, save and restore 10 discovered APIs, 10 saved profiles, 10 access tokens.
        // Should be still fast.
        String baseURI = "http://example.com/baseURI";
        for (int i = 0; i < 10; ++i) {
            DiscoveredAPI discoveredAPI =  new DiscoveredAPI("http://example.com/", "http://example.com/auth_endpoint",
                    "http://example.com/token_endpoint");
            _historyService.cacheDiscoveredAPI(baseURI + i, discoveredAPI);
            String profileId = "vpn_profile";
            String profileUUID = "ABCD-1234-DEFG-5678";
            Instance instance = new Instance(baseURI + i, "displayName", null, AuthorizationType.DISTRIBUTED, true);
            Profile profile = new Profile("displayName", profileId);
            SavedProfile savedProfile = new SavedProfile(instance, profile, profileUUID);
            _historyService.cacheSavedProfile(savedProfile);
            _historyService.cacheAuthenticationState(instance, new AuthState());
        }
        _reloadHistoryService(false);
        assertEquals(10, _historyService.getSavedProfileList().size());
        for (int i = 0; i < 10; ++i) {
            assertNotNull(_historyService.getCachedAuthState(new Instance(baseURI + i, "displayName", null, AuthorizationType.DISTRIBUTED, true)));
            assertNotNull(_historyService.getCachedDiscoveredAPI(baseURI + i));
        }

    }

    @Test
    public void testCacheDiscoveredAPI() {
        String baseUri = "http://example.com";
        DiscoveredAPI discoveredAPI = new DiscoveredAPI("http://example.com/", "http://example.com/auth_endpoint",
                "http://example.com/token_endpoint");
        _historyService.cacheDiscoveredAPI(baseUri, discoveredAPI);
        _reloadHistoryService(false);
        DiscoveredAPI restoredDiscoveredAPI = _historyService.getCachedDiscoveredAPI(baseUri);
        assertNotNull(restoredDiscoveredAPI);
        assertEquals(discoveredAPI.getAuthorizationEndpoint(), restoredDiscoveredAPI.getAuthorizationEndpoint());
        // We could test the other properties as well, but that is already tested by the serializer service.
    }

    @Test
    public void testCacheAccessToken() {
        String baseURI = "http://example.com";
        AuthState exampleAuthState = new AuthState(new AuthorizationServiceConfiguration(Uri.parse("http://example.com/auth"), Uri.parse("http://example.com/token"), null));
        Instance instance = new Instance(baseURI, "displayName", null, AuthorizationType.DISTRIBUTED, true);
        _historyService.cacheAuthenticationState(instance, exampleAuthState);
        _reloadHistoryService(false);
        AuthState restoredAuthState = _historyService.getCachedAuthState(instance);
        //noinspection ConstantConditions
        assertEquals(exampleAuthState.getAuthorizationServiceConfiguration().authorizationEndpoint, restoredAuthState.getAuthorizationServiceConfiguration().authorizationEndpoint);
        assertEquals(exampleAuthState.getAuthorizationServiceConfiguration().tokenEndpoint, restoredAuthState.getAuthorizationServiceConfiguration().tokenEndpoint);
    }

    @Test
    public void testCacheAndRemoveSavedProfile() {
        String baseURI = "http://example.com/baseURI";
        String profileId = "vpn_profile";
        String profileUUID = "ABCD-1234-DEFG-5678";
        Instance instance = new Instance(baseURI, "displayName", null, AuthorizationType.DISTRIBUTED, true);
        Profile profile = new Profile("displayName", profileId);
        SavedProfile savedProfile = new SavedProfile(instance, profile, profileUUID);
        _historyService.cacheSavedProfile(savedProfile);
        _reloadHistoryService(false);
        SavedProfile restoredProfile = _historyService.getCachedSavedProfile(instance.getSanitizedBaseURI(), profileId);
        assertNotNull(restoredProfile);
        assertEquals(savedProfile.getProfileUUID(), restoredProfile.getProfileUUID());
        // Now test if it can be removed correctly
        _historyService.removeSavedProfile(restoredProfile);
        SavedProfile removedProfile = _historyService.getCachedSavedProfile(instance.getSanitizedBaseURI(), profileId);
        // Since it was removed, it should be null
        assertNull(removedProfile);
        // Also make sure it stays removed after a reload
        _reloadHistoryService(false);
        removedProfile = _historyService.getCachedSavedProfile(instance.getSanitizedBaseURI(), profileId);
        assertNull(removedProfile);
    }

    @Test
    public void testStoreSavedKeyPair() {
        KeyPair keyPair1 = new KeyPair(false, "cert1", "pk1");
        Instance instance1 = new Instance("http://example.com/", "example.com", null, AuthorizationType.DISTRIBUTED, false);
        SavedKeyPair savedKeyPair1 = new SavedKeyPair(instance1, keyPair1);
        Instance instance2 = new Instance("http://something.else/", "something.else", null, AuthorizationType.DISTRIBUTED, false);
        KeyPair keyPair2 = new KeyPair(true, "example certificate", "example private key");
        SavedKeyPair savedKeyPair2 = new SavedKeyPair(instance2, keyPair2);
        _historyService.storeSavedKeyPair(savedKeyPair1);
        _historyService.storeSavedKeyPair(savedKeyPair2);
        _reloadHistoryService(false);
        SavedKeyPair retrieved1 = _historyService.getSavedKeyPairForInstance(instance1);
        SavedKeyPair retrieved2 = _historyService.getSavedKeyPairForInstance(instance2);
        assertNotNull(retrieved1);
        assertNotNull(retrieved2);
        assertEquals(keyPair1.isOK(), retrieved1.getKeyPair().isOK());
        assertEquals(keyPair1.getCertificate(), retrieved1.getKeyPair().getCertificate());
        assertEquals(keyPair1.getPrivateKey(), retrieved1.getKeyPair().getPrivateKey());
        assertEquals(keyPair2.isOK(), retrieved2.getKeyPair().isOK());
        assertEquals(keyPair2.getCertificate(), retrieved2.getKeyPair().getCertificate());
        assertEquals(keyPair2.getPrivateKey(), retrieved2.getKeyPair().getPrivateKey());
    }
}