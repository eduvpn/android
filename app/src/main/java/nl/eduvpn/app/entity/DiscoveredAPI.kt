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
package nl.eduvpn.app.entity

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.Constants

/**
 * A discovered API entity, containing all the URLs.
 * Created by Daniel Zolnai on 2016-10-18.
 */

sealed class DiscoveredAPI {
    abstract val authorizationEndpoint: String
    abstract val tokenEndpoint: String

    fun toDiscoveredAPIs(): DiscoveredAPIs {
        return when (this) {
            is DiscoveredAPIV2 -> DiscoveredAPIs(this, null)
            is DiscoveredAPIV3 -> DiscoveredAPIs(null, this)
        }
    }

    internal fun getURL(baseURI: String, path: String): String =
        Uri.parse(baseURI)
            .buildUpon()
            .appendPath(path)
            .build()
            .toString()
}

@Serializable
class DiscoveredAPIV3(

    @SerialName("api_endpoint")
    val apiEndpoint: String,

    @SerialName("authorization_endpoint")
    override val authorizationEndpoint: String,

    @SerialName("token_endpoint")
    override val tokenEndpoint: String
) : DiscoveredAPI() {

    val infoEndpoint: String by lazy {
        getURL(apiEndpoint, Constants.API_V3_INFO_PATH)
    }

    val connectEndpoint: String by lazy {
        getURL(apiEndpoint, Constants.API_V3_CONNECT_PATH)
    }

    val disconnectEndpoint: String by lazy {
        getURL(apiEndpoint, Constants.API_V3_DISCONNECT_PATH)
    }
}

@Serializable
class DiscoveredAPIV2(
    @SerialName("api_base_uri")
    val apiBaseUri: String,

    @SerialName("authorization_endpoint")
    override val authorizationEndpoint: String,

    @SerialName("token_endpoint")
    override val tokenEndpoint: String
) : DiscoveredAPI() {

    val profileListEndpoint: String by lazy {
        getURL(apiBaseUri, Constants.API_PROFILE_LIST_PATH)
    }

    val createKeyPairEndpoint: String by lazy {
        getURL(apiBaseUri, Constants.API_CREATE_KEYPAIR)
    }

    val profileConfigEndpoint: String by lazy {
        getURL(apiBaseUri, Constants.API_PROFILE_CONFIG)
    }

    fun getCheckCertificateEndpoint(certCommonName: String?): String {
        return Uri.parse(apiBaseUri).buildUpon()
            .appendPath(Constants.API_CHECK_CERTIFICATE)
            .appendQueryParameter("common_name", certCommonName)
            .build()
            .toString()
    }
}
