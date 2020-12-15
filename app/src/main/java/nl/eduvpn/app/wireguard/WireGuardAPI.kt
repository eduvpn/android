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

package nl.eduvpn.app.wireguard

import android.net.Uri
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import com.wireguard.crypto.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import nl.eduvpn.app.service.APIService
import nl.eduvpn.app.service.APIService.UserNotAuthorizedException
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.net.URLEncoder

class WireGuardAPI(private val apiService: APIService,
                   private val baseURI: String) {

    class WireGuardAPIException(message: String, exception: Exception) : Exception(message, exception)

    /**
     * @throws UserNotAuthorizedException
     * @throws IOException
     */
    suspend fun wireGuardEnabled(authState: AuthState?): Boolean {
        //todo: checking response code does not seem to be possible with .getString
        val string: String = apiService.getString(getURL("available"), authState)
        return string == "y"
    }

    /**
     * @throws IOException
     * @throws UserNotAuthorizedException
     * @throws WireGuardAPIException
     */
    suspend fun createConfig(authState: AuthState?): Config {
        val keyPair = com.wireguard.crypto.KeyPair()
        val configString = createConfig(keyPair.publicKey, authState)
        val configStringWithPrivateKey = configString
                .replace("[Interface]",
                        "[Interface]\n" +
                                "PrivateKey = ${keyPair.privateKey.toBase64()}")

        //todo: parse stream
        val config = withContext(Dispatchers.IO) {
            try {
                Config.parse(BufferedReader(StringReader(configStringWithPrivateKey)))
            } catch (ex: BadConfigException) {
                throw WireGuardAPIException("Error parsing WireGuard config.", ex)
            }
        }
        return config
    }

    /**
     * @throws UserNotAuthorizedException
     * @throws IOException
     */
    private suspend fun createConfig(publicKey: Key, authSate: AuthState?): String {
        val configString: String = apiService.postResource(
                getURL("create_config"),
                "publicKey=" + URLEncoder.encode(publicKey.toBase64(), Charsets.UTF_8.name()),
                authSate)

        return configString
    }

    private fun getURL(path: String): String = Uri.parse(baseURI)
            .buildUpon().appendPath("wg")
            .appendPath(path)
            .build().toString()
}
