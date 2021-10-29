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
        Uri.parse(apiEndpoint).buildUpon().appendPath(Constants.API_V3_INFO_PATH).build()
            .toString()
    }

    val connectEndpoint: String by lazy {
        Uri.parse(apiEndpoint).buildUpon().appendPath(Constants.API_V3_CONNECT_PATH).build()
            .toString()
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

    val systemMessagesEndpoint: String by lazy {
        Uri.parse(apiBaseUri).buildUpon().appendPath(Constants.API_SYSTEM_MESSAGES_PATH).build()
            .toString()
    }

    val userMessagesEndpoint: String by lazy {
        Uri.parse(apiBaseUri).buildUpon().appendPath(Constants.API_USER_MESSAGES_PATH).build()
            .toString()
    }

    val profileListEndpoint: String by lazy {
        Uri.parse(apiBaseUri).buildUpon().appendPath(Constants.API_PROFILE_LIST_PATH).build()
            .toString()
    }

    val createKeyPairEndpoint: String by lazy {
        Uri.parse(apiBaseUri).buildUpon().appendPath(Constants.API_CREATE_KEYPAIR).build()
            .toString()
    }

    val profileConfigEndpoint: String by lazy {
        Uri.parse(apiBaseUri).buildUpon().appendPath(Constants.API_PROFILE_CONFIG).build()
            .toString()
    }

    fun getCheckCertificateEndpoint(certCommonName: String?): String {
        return Uri.parse(apiBaseUri).buildUpon()
            .appendPath(Constants.API_CHECK_CERTIFICATE)
            .appendQueryParameter("common_name", certCommonName)
            .build()
            .toString()
    }
}
