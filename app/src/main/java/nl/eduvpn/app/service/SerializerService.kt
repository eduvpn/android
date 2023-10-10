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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import nl.eduvpn.app.entity.AddedServers
import nl.eduvpn.app.entity.CertExpiryTimes
import nl.eduvpn.app.entity.CookieAndProfileMapData
import nl.eduvpn.app.entity.CookieAndStringData
import nl.eduvpn.app.entity.CurrentServer
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.entity.OrganizationList
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.entity.SerializedVpnConfig
import nl.eduvpn.app.entity.ServerList
import nl.eduvpn.app.entity.Settings
import nl.eduvpn.app.entity.TranslatableString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * This service is responsible for (de)serializing objects used in the app.
 * Created by Daniel Zolnai on 2016-10-12.
 */
class SerializerService {
    class UnknownFormatException internal constructor(throwable: Throwable?) : Exception(throwable)

    @Throws(UnknownFormatException::class)
    fun deserializeProfileList(json: String?): List<Profile> {
        return try {
            jsonSerializer.decodeFromString(ListSerializer(Profile.serializer()), json!!)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Serializes an instance to a JSON format.
     *
     * @param instance The instance to serialize.
     * @return The JSON object if the serialization was successful.
     * @throws UnknownFormatException Thrown if there was an error.
     */
    @Throws(UnknownFormatException::class)
    fun serializeInstance(instance: Instance): String {
        return try {
            jsonSerializer.encodeToString(Instance.serializer(), instance)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Deserializes an instance object from a JSON.
     *
     * @param json The JSON object to parse.
     * @return The instance as a POJO.
     * @throws UnknownFormatException Thrown when the format was not as expected.
     */
    @Throws(UnknownFormatException::class)
    fun deserializeInstance(json: String?): Instance {
        return try {
            jsonSerializer.decodeFromString(Instance.serializer(), json!!)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Deserializes the app settings from JSON to POJO.
     *
     * @param jsonObject The json containing the settings.
     * @return The settings as an object.
     * @throws UnknownFormatException Thrown if there was a problem while parsing the JSON.
     */
    @Throws(UnknownFormatException::class)
    fun deserializeAppSettings(jsonObject: JSONObject): Settings {
        return try {
            var useCustomTabs = Settings.USE_CUSTOM_TABS_DEFAULT_VALUE
            if (jsonObject.has("use_custom_tabs")) {
                useCustomTabs = jsonObject.getBoolean("use_custom_tabs")
            }
            var forceTcp = Settings.FORCE_TCP_DEFAULT_VALUE
            if (jsonObject.has("force_tcp")) {
                forceTcp = jsonObject.getBoolean("force_tcp")
            }
            Settings(useCustomTabs, forceTcp)
        } catch (ex: JSONException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Serializes the app settings to JSON.
     *
     * @param settings The settings to serialize.
     * @return The app settings in a JSON format.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    @Throws(UnknownFormatException::class)
    fun serializeAppSettings(settings: Settings): JSONObject {
        val result = JSONObject()
        return try {
            result.put("use_custom_tabs", settings.useCustomTabs())
            result.put("force_tcp", settings.forceTcp())
            result
        } catch (ex: JSONException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Serializes an organization into a JSON object.
     *
     * @param organization The organization to serialize
     * @return The organization as a JSON object.
     * @throws UnknownFormatException Thrown if there was an error while serializing.
     */
    @Throws(UnknownFormatException::class)
    fun serializeOrganization(organization: Organization): JSONObject {
        val result = JSONObject()
        try {
            result.put("org_id", organization.orgId)
            if (organization.displayName.translations.isEmpty()) {
                result.put("display_name", null)
            } else {
                val translations = JSONObject()
                for ((key, value) in organization.displayName.translations) {
                    translations.put(key, value)
                }
                result.put("display_name", translations)
            }
            if (organization.keywordList.translations.isEmpty()) {
                result.put("keyword_list", null)
            } else {
                val translations = JSONObject()
                for ((key, value) in organization.keywordList.translations) {
                    translations.put(key, value)
                }
                result.put("keyword_list", translations)
            }
            result.put("secure_internet_home", organization.secureInternetHome)
        } catch (ex: JSONException) {
            throw UnknownFormatException(ex)
        }
        return result
    }

    /**
     * Deserializes an organization from JSON.
     *
     * @param jsonObject The JSON object to deserialize.
     * @return The organization instance.
     * @throws UnknownFormatException Thrown if the JSON has an unknown format.
     */
    @Throws(UnknownFormatException::class)
    fun deserializeOrganization(jsonObject: JSONObject): Organization {
        return try {
            val displayName: TranslatableString
            displayName = if (jsonObject.isNull("display_name")) {
                TranslatableString()
            } else if (jsonObject["display_name"] is String) {
                TranslatableString(jsonObject.getString("display_name"))
            } else {
                _parseAllTranslations(jsonObject.getJSONObject("display_name"))
            }
            val orgId = jsonObject.getString("org_id")
            val translatableString: TranslatableString =
                if (jsonObject.has("keyword_list") && !jsonObject.isNull("keyword_list")) {
                    if (jsonObject["keyword_list"] is JSONObject) {
                        _parseAllTranslations(jsonObject.getJSONObject("keyword_list"))
                    } else if (jsonObject["keyword_list"] is String) {
                        TranslatableString(jsonObject.getString("keyword_list"))
                    } else {
                        throw JSONException("keyword_list should be object or string")
                    }
                } else {
                    TranslatableString()
                }
            var secureInternetHome: String? = null
            if (jsonObject.has("secure_internet_home") && !jsonObject.isNull("secure_internet_home")) {
                secureInternetHome = jsonObject.getString("secure_internet_home")
            }
            Organization(orgId, displayName, translatableString, secureInternetHome)
        } catch (ex: JSONException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Deserializes a list of organizations.
     *
     * @param jsonObject The json to deserialize from.
     * @return The list of organizations created from the JSON.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    @Throws(UnknownFormatException::class)
    fun deserializeOrganizationList(jsonObject: JSONObject): OrganizationList {
        return try {
            val result: MutableList<Organization> = ArrayList()
            val version = jsonObject.getLong("v")
            val itemsList = jsonObject.getJSONArray("organization_list")
            for (i in 0 until itemsList.length()) {
                val serializedItem = itemsList.getJSONObject(i)
                val organization = deserializeOrganization(serializedItem)
                result.add(organization)
            }
            OrganizationList(version, result)
        } catch (ex: JSONException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Serializes a list of organizations into a JSON format.
     *
     * @param organizationList The list of organizations to serialize.
     * @return The messages as a JSON object.
     * @throws UnknownFormatException Thrown if there was an error constructing the JSON.
     */
    @Throws(UnknownFormatException::class)
    fun serializeOrganizationList(organizationList: OrganizationList): JSONObject {
        return try {
            val result = JSONObject()
            val organizationsArray = JSONArray()
            for (organization in organizationList.organizationList) {
                organizationsArray.put(serializeOrganization(organization))
            }
            result.put("organization_list", organizationsArray)
            result.put("v", organizationList.version)
            result
        } catch (ex: JSONException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Deserializes a list of organization servers.
     *
     * @param json The json to deserialize from.
     * @return The list of organizations servers created from the JSON.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    @Throws(UnknownFormatException::class)
    fun deserializeServerList(json: String?): ServerList {
        return try {
            jsonSerializer.decodeFromString(ServerList.serializer(), json!!)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Serializes the server list to JSON format
     *
     * @param serverList The server list to serialize.
     * @return The server list as a JSON object.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    @Throws(UnknownFormatException::class)
    fun serializeServerList(serverList: ServerList): String {
        return try {
            jsonSerializer.encodeToString(ServerList.serializer(), serverList)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    /**
     * Retrieves the translations from a JSON object.
     *
     * @param translationsObject The JSON object to retrieve the translations from.
     * @return A TranslatableString instance.
     * @throws JSONException Thrown if the input is in an unexpected format.
     */
    @Throws(JSONException::class)
    private fun _parseAllTranslations(translationsObject: JSONObject): TranslatableString {
        val keysIterator = translationsObject.keys()
        val translationsMap: MutableMap<String, String> = HashMap()
        while (keysIterator.hasNext()) {
            val key = keysIterator.next()
            val value = translationsObject.getString(key)
            translationsMap[key] = value
        }
        return TranslatableString(translationsMap)
    }

    @Throws(UnknownFormatException::class)
    fun deserializeCookieAndStringData(json: String?): CookieAndStringData {
        return try {
            jsonSerializer.decodeFromString(CookieAndStringData.serializer(), json!!)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    @Throws(UnknownFormatException::class)
    fun deserializeCookieAndCookieAndProfileListData(json: String?): CookieAndProfileMapData {
        return try {
            jsonSerializer.decodeFromString(CookieAndProfileMapData.serializer(), json!!)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    @Throws(UnknownFormatException::class)
    fun deserializeAddedServers(json: String?): AddedServers {
        return try {
            jsonSerializer.decodeFromString(AddedServers.serializer(), json!!)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    @Throws(UnknownFormatException::class)
    fun deserializeSerializedVpnConfig(json: String?): SerializedVpnConfig {
        return try {
            jsonSerializer.decodeFromString(SerializedVpnConfig.serializer(), json!!)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    @Throws(UnknownFormatException::class)
    fun deserializeCurrentServer(json: String): CurrentServer {
        return try {
            jsonSerializer.decodeFromString(CurrentServer.serializer(), json)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    @Throws(UnknownFormatException::class)
    fun deserializeCertExpiryTimes(json: String): CertExpiryTimes {
        return try {
            jsonSerializer.decodeFromString(CertExpiryTimes.serializer(), json)
        } catch (ex: SerializationException) {
            throw UnknownFormatException(ex)
        }
    }

    companion object {
        private val API_DATE_FORMAT: DateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        private val jsonSerializer: Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}