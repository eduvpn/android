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

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import nl.eduvpn.app.entity.*
import org.junit.*
import org.junit.runner.RunWith
import java.util.*

/**
 * Tests for the history service.
 * Created by Daniel Zolnai on 2016-10-22.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HistoryServiceTest {

    private var _historyService: HistoryService? = null

    @Before
    @After
    @Throws(Exception::class)
    fun clearPrefs() {
        reloadHistoryService(true)
    }

    /**
     * Reloads the service, so we can test if the (de)serialization works as expected.
     *
     * @param clearHistory If the history should be cleared beforehand,
     */
    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    private fun reloadHistoryService(clearHistory: Boolean) {
        val serializerService = SerializerService()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferencesService = PreferencesService(context, serializerService)
        // Clean the shared preferences if needed
        if (clearHistory) {
            preferencesService.clearPreferences()
        } else {
            // By doing a new commit, we make sure that all other pending transactions are being taken care of
            preferencesService.getSharedPreferences()
                .edit()
                .putInt("DUMMY_KEY", Random().nextInt())
                .commit()
        }
        _historyService = HistoryService(preferencesService)
    }

    @Test(timeout = 1000) // Could be a lot faster, but we use secure preferences, which encrypts and decrypts on-the-fly.
    fun testSerializationSpeed() {
        // We create, save and restore 10 discovered APIs and 10 access tokens.
        // Should be still fast.
        val baseURI = "http://example.com/baseURI"
        for (i in 0..9) {
            val instance = Instance(
                baseURI + i,
                TranslatableString("displayName"),
                null,
                AuthorizationType.Distributed,
                "NL",
                true,
                "https://example.com/template",
                ArrayList()
            )
            _historyService!!.cacheAuthorizationState(instance, AuthState(), Date())
        }
        reloadHistoryService(false)
        for (i in 0..9) {
            Assert.assertNotNull(
                _historyService!!.getCachedAuthState(
                    Instance(
                        baseURI + i,
                        TranslatableString("displayName"),
                        null,
                        AuthorizationType.Distributed,
                        "NL",
                        true,
                        null,
                        ArrayList()
                    )
                )
            )
        }
    }

    @Test
    fun testCacheAccessToken() {
        val baseURI = "http://example.com"
        val exampleAuthState = AuthState(
            AuthorizationServiceConfiguration(
                Uri.parse("http://example.com/auth"), Uri
                    .parse("http://example.com/token"), null
            )
        )
        val instance = Instance(
            baseURI,
            TranslatableString("displayName"),
            null,
            AuthorizationType.Distributed,
            "HU",
            true,
            "https://eduvpn.org/template",
            ArrayList()
        )
        val now = Date()
        _historyService!!.cacheAuthorizationState(instance, exampleAuthState, now)
        reloadHistoryService(false)
        val (restoredAuthState, _) = _historyService!!.getCachedAuthState(instance)!!
        Assert.assertEquals(
            exampleAuthState.authorizationServiceConfiguration!!.authorizationEndpoint,
            restoredAuthState
                .authorizationServiceConfiguration!!.authorizationEndpoint
        )
        Assert.assertEquals(
            exampleAuthState.authorizationServiceConfiguration!!.tokenEndpoint, restoredAuthState
                .authorizationServiceConfiguration!!.tokenEndpoint
        )
    }

    @Test
    fun testStoreSavedKeyPair() {
        val keyPair1 = KeyPair(false, "cert1", "pk1")
        val instance1 = Instance(
            "http://example.com/",
            TranslatableString("example.com"),
            null,
            AuthorizationType.Distributed,
            "DK",
            false,
            "https://eduvpn.org/template",
            ArrayList()
        )
        val savedKeyPair1 = SavedKeyPair(instance1, keyPair1)
        val instance2 = Instance(
            "http://something.else/",
            TranslatableString("something.else"),
            null,
            AuthorizationType.Distributed,
            "DK",
            false,
            null,
            ArrayList()
        )
        val keyPair2 = KeyPair(true, "example certificate", "example private key")
        val savedKeyPair2 = SavedKeyPair(instance2, keyPair2)
        _historyService!!.storeSavedKeyPair(savedKeyPair1)
        _historyService!!.storeSavedKeyPair(savedKeyPair2)
        reloadHistoryService(false)
        val retrieved1 = _historyService!!.getSavedKeyPairForInstance(instance1)
        val retrieved2 = _historyService!!.getSavedKeyPairForInstance(instance2)
        Assert.assertNotNull(retrieved1)
        Assert.assertNotNull(retrieved2)
        Assert.assertEquals(keyPair1.isOK, retrieved1.keyPair.isOK)
        Assert.assertEquals(keyPair1.certificate, retrieved1.keyPair.certificate)
        Assert.assertEquals(keyPair1.privateKey, retrieved1.keyPair.privateKey)
        Assert.assertEquals(keyPair2.isOK, retrieved2.keyPair.isOK)
        Assert.assertEquals(keyPair2.certificate, retrieved2.keyPair.certificate)
        Assert.assertEquals(keyPair2.privateKey, retrieved2.keyPair.privateKey)
    }
}
