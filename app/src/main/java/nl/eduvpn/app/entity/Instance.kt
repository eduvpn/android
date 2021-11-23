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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.Serializer.TranslatableStringSerializer

/**
 * A configuration for an instance.
 * Created by Daniel Zolnai on 2016-10-07.
 */
@Serializable
data class Instance(

    @SerialName("base_url")
    val baseURI: String,

    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString = TranslatableString(),

    @SerialName("logo")
    val logoUri: String? = null,

    @SerialName("server_type")
    val authorizationType: AuthorizationType = AuthorizationType.Local, //todo: do not crash if unknown authorization type but use default one

    @SerialName("country_code")
    val countryCode: String? = null,

    @SerialName("is_custom")
    val isCustom: Boolean = false,

    @SerialName("authentication_url_template")
    val authenticationUrlTemplate: String? = null,

    @SerialName("support_contact")
    val supportContact: List<String> = emptyList()
) {

    val sanitizedBaseURI: String
        get() = if (baseURI.endsWith("/")) {
            baseURI.substring(0, baseURI.length - 1)
        } else baseURI

}
