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

import static kotlinx.serialization.builtins.BuiltinSerializersKt.ListSerializer;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import kotlin.Unit;
import kotlinx.serialization.SerializationException;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonKt;
import nl.eduvpn.app.entity.AddedServers;
import nl.eduvpn.app.entity.CookieAndProfileMapData;
import nl.eduvpn.app.entity.CookieAndStringData;
import nl.eduvpn.app.entity.DiscoveredAPIs;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.KeyPair;
import nl.eduvpn.app.entity.Organization;
import nl.eduvpn.app.entity.OrganizationList;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedKeyPairList;
import nl.eduvpn.app.entity.SerializedVpnConfig;
import nl.eduvpn.app.entity.ServerList;
import nl.eduvpn.app.entity.Settings;
import nl.eduvpn.app.entity.TranslatableString;
import nl.eduvpn.app.entity.WellKnown;
import nl.eduvpn.app.entity.v3.Info;
import nl.eduvpn.app.entity.v3.ProfileV3API;
import nl.eduvpn.app.entity.v3.Protocol;
import nl.eduvpn.app.utils.serializer.KeyPairSerializer;

/**
 * This service is responsible for (de)serializing objects used in the app.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class SerializerService {

    public static class UnknownFormatException extends Exception {
        UnknownFormatException(Throwable throwable) {
            super(throwable);
        }
    }

    private static final DateFormat API_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private static final Json jsonSerializer = JsonKt.Json(Json.Default, (j) -> {
        j.setIgnoreUnknownKeys(true);
        j.setCoerceInputValues(true);
        return Unit.INSTANCE;
    });

    static {
        API_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Serialized a list of profiles to JSON Stirng
     *
     * @param profileList The list of profiles to serialize.
     * @return The list of profiles in a JSON array.
     * @throws UnknownFormatException Thrown if there was a problem while creating the JSON.
     */
    public String serializeProfileList(@NonNull List<Profile> profileList) throws UnknownFormatException {
        try {
            return jsonSerializer.encodeToString(ListSerializer(Profile.Companion.serializer()), profileList);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    public List<ProfileV3API> deserializeProfileList(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(ListSerializer(ProfileV3API.Companion.serializer()), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a profile to JSON.
     *
     * @param profile The profile to serialize.
     * @return The profile in a JSON format.
     */
    public String serializeProfile(Profile profile) throws UnknownFormatException {
        try {
            return jsonSerializer.encodeToString(Profile.Companion.serializer(), profile);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a profile JSON to an object instance.
     *
     * @param json The JSON to deserialize.
     * @return The profile as a POJO
     * @throws UnknownFormatException Thrown if the format was unknown.
     */
    public Profile deserializeProfile(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(Profile.Companion.serializer(), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes an instance to a JSON format.
     *
     * @param instance The instance to serialize.
     * @return The JSON object if the serialization was successful.
     * @throws UnknownFormatException Thrown if there was an error.
     */
    public String serializeInstance(Instance instance) throws UnknownFormatException {
        try {
            return jsonSerializer.encodeToString(Instance.Companion.serializer(), instance);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes an instance object from a JSON.
     *
     * @param json The JSON object to parse.
     * @return The instance as a POJO.
     * @throws UnknownFormatException Thrown when the format was not as expected.
     */
    public Instance deserializeInstance(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(Instance.Companion.serializer(), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    public Info deserializeInfo(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(Info.Companion.serializer(), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a JSON object containing the discovered APIs endpoints.
     *
     * @param result The JSON object to deserialize
     * @return The discovered APIs object.
     * @throws UnknownFormatException Thrown if the JSON had an unknown format.
     */
    @NonNull
    public DiscoveredAPIs deserializeDiscoveredAPIs(String result) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(WellKnown.Companion.serializer(), result)
                    .getApi();
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a discovered APIs object.
     *
     * @param discoveredAPI The object to serialize to JSON.
     * @return The object as a JSON string.
     * @throws UnknownFormatException Thrown if there was an error while parsing.
     */
    public String serializeDiscoveredAPIs(DiscoveredAPIs discoveredAPI) throws UnknownFormatException {
        try {
            return jsonSerializer.encodeToString(WellKnown.Companion.serializer(), new WellKnown(discoveredAPI));
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes the app settings from JSON to POJO.
     *
     * @param jsonObject The json containing the settings.
     * @return The settings as an object.
     * @throws UnknownFormatException Thrown if there was a problem while parsing the JSON.
     */
    public Settings deserializeAppSettings(JSONObject jsonObject) throws UnknownFormatException {
        try {
            boolean useCustomTabs = Settings.USE_CUSTOM_TABS_DEFAULT_VALUE;
            if (jsonObject.has("use_custom_tabs")) {
                useCustomTabs = jsonObject.getBoolean("use_custom_tabs");
            }
            boolean forceTcp = Settings.FORCE_TCP_DEFAULT_VALUE;
            if (jsonObject.has("force_tcp")) {
                forceTcp = jsonObject.getBoolean("force_tcp");
            }
            return new Settings(useCustomTabs, forceTcp);
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes the app settings to JSON.
     *
     * @param settings The settings to serialize.
     * @return The app settings in a JSON format.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public JSONObject serializeAppSettings(Settings settings) throws UnknownFormatException {
        JSONObject result = new JSONObject();
        try {
            result.put("use_custom_tabs", settings.useCustomTabs());
            result.put("force_tcp", settings.forceTcp());
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a key pair object from a JSON.
     *
     * @param json The json representation of the key pair.
     * @return The keypair instance if succeeded.
     * @throws UnknownFormatException Thrown when the format of the JSON does not match the app format.
     */
    public KeyPair deserializeKeyPair(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(KeyPairSerializer.INSTANCE, json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a key pair into json.
     *
     * @param keyPair The key pair to serialize.
     * @return The JSON representation of the key pair.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public String serializeKeyPair(KeyPair keyPair) throws UnknownFormatException {
        try {
            return jsonSerializer.encodeToString(KeyPairSerializer.INSTANCE, keyPair);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a list of saved key pairs.
     *
     * @param savedKeyPairList The key pair list to serialize.
     * @return The key pair list serialized to JSON format.
     * @throws UnknownFormatException Thrown if there was an error while serializing.
     */
    public String serializeSavedKeyPairList(List<SavedKeyPair> savedKeyPairList) throws UnknownFormatException {
        try {
            return jsonSerializer.encodeToString(SavedKeyPairList.Companion.serializer(), new SavedKeyPairList(savedKeyPairList));
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a list of saved key pairs.
     *
     * @param json The json to deserialize from.
     * @return The list of saved key pairs created from the JSON.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public List<SavedKeyPair> deserializeSavedKeyPairList(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(SavedKeyPairList.Companion.serializer(), json)
                    .getItems();
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes an organization into a JSON object.
     *
     * @param organization The organization to serialize
     * @return The organization as a JSON object.
     * @throws UnknownFormatException Thrown if there was an error while serializing.
     */
    public JSONObject serializeOrganization(Organization organization) throws UnknownFormatException {
        JSONObject result = new JSONObject();
        try {
            result.put("org_id", organization.getOrgId());
            if (organization.getDisplayName().getTranslations().isEmpty()) {
                result.put("display_name", null);
            } else {
                JSONObject translations = new JSONObject();
                for (Map.Entry<String, String> entry : organization.getDisplayName().getTranslations().entrySet()) {
                    translations.put(entry.getKey(), entry.getValue());
                }
                result.put("display_name", translations);
            }
            if (organization.getKeywordList().getTranslations().isEmpty()) {
                result.put("keyword_list", null);
            } else {
                JSONObject translations = new JSONObject();
                for (Map.Entry<String, String> entry : organization.getKeywordList().getTranslations().entrySet()) {
                    translations.put(entry.getKey(), entry.getValue());
                }
                result.put("keyword_list", translations);
            }
            result.put("secure_internet_home", organization.getSecureInternetHome());
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
        return result;
    }

    /**
     * Deserializes an organization from JSON.
     *
     * @param jsonObject The JSON object to deserialize.
     * @return The organization instance.
     * @throws UnknownFormatException Thrown if the JSON has an unknown format.
     */
    public Organization deserializeOrganization(JSONObject jsonObject) throws UnknownFormatException {
        try {
            TranslatableString displayName;
            if (jsonObject.isNull("display_name")) {
                displayName = new TranslatableString();
            } else if (jsonObject.get("display_name") instanceof String) {
                displayName = new TranslatableString(jsonObject.getString("display_name"));
            } else {
                displayName = _parseAllTranslations(jsonObject.getJSONObject("display_name"));
            }
            String orgId = jsonObject.getString("org_id");
            TranslatableString translatableString;
            if (jsonObject.has("keyword_list") && !jsonObject.isNull("keyword_list")) {
                if (jsonObject.get("keyword_list") instanceof JSONObject) {
                    translatableString = _parseAllTranslations(jsonObject.getJSONObject("keyword_list"));
                } else if (jsonObject.get("keyword_list") instanceof String) {
                    translatableString = new TranslatableString(jsonObject.getString("keyword_list"));
                } else {
                    throw new JSONException("keyword_list should be object or string");
                }
            } else {
                translatableString = new TranslatableString();
            }
            String secureInternetHome = null;
            if (jsonObject.has("secure_internet_home") && !jsonObject.isNull("secure_internet_home")) {
                secureInternetHome = jsonObject.getString("secure_internet_home");
            }
            return new Organization(orgId, displayName, translatableString, secureInternetHome);
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a list of organizations.
     *
     * @param jsonObject The json to deserialize from.
     * @return The list of organizations created from the JSON.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public OrganizationList deserializeOrganizationList(JSONObject jsonObject) throws UnknownFormatException {
        try {
            List<Organization> result = new ArrayList<>();
            long version = jsonObject.getLong("v");
            JSONArray itemsList = jsonObject.getJSONArray("organization_list");
            for (int i = 0; i < itemsList.length(); ++i) {
                JSONObject serializedItem = itemsList.getJSONObject(i);
                Organization organization = deserializeOrganization(serializedItem);
                result.add(organization);
            }
            return new OrganizationList(version, result);
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a list of organizations into a JSON format.
     *
     * @param organizationList The list of organizations to serialize.
     * @return The messages as a JSON object.
     * @throws UnknownFormatException Thrown if there was an error constructing the JSON.
     */
    public JSONObject serializeOrganizationList(OrganizationList organizationList) throws UnknownFormatException {
        try {
            JSONObject result = new JSONObject();
            JSONArray organizationsArray = new JSONArray();
            for (Organization organization : organizationList.getOrganizationList()) {
                organizationsArray.put(serializeOrganization(organization));
            }
            result.put("organization_list", organizationsArray);
            result.put("v", organizationList.getVersion());
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a list of organization servers.
     *
     * @param jsonObject The json to deserialize from.
     * @return The list of organizations servers created from the JSON.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public ServerList deserializeServerList(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(ServerList.Companion.serializer(), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes the server list to JSON format
     *
     * @param serverList The server list to serialize.
     * @return The server list as a JSON object.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public String serializeServerList(ServerList serverList) throws UnknownFormatException {
        try {
            return jsonSerializer.encodeToString(ServerList.Companion.serializer(), serverList);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Retrieves the translations from a JSON object.
     *
     * @param translationsObject The JSON object to retrieve the translations from.
     * @return A TranslatableString instance.
     * @throws JSONException Thrown if the input is in an unexpected format.
     */
    private TranslatableString _parseAllTranslations(JSONObject translationsObject) throws JSONException {
        Iterator<String> keysIterator = translationsObject.keys();
        Map<String, String> translationsMap = new HashMap<>();
        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            String value = translationsObject.getString(key);
            translationsMap.put(key, value);
        }
        return new TranslatableString(translationsMap);
    }

    public CookieAndStringData deserializeCookieAndStringData(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(CookieAndStringData.Companion.serializer(), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    public CookieAndProfileMapData deserializeCookieAndCookieAndProfileListData(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(CookieAndProfileMapData.Companion.serializer(), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    public AddedServers deserializeAddedServers(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(AddedServers.Companion.serializer(), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    public SerializedVpnConfig deserializeSerializedVpnConfig(String json) throws UnknownFormatException {
        try {
            return jsonSerializer.decodeFromString(SerializedVpnConfig.Companion.serializer(), json);
        } catch (SerializationException ex) {
            throw new UnknownFormatException(ex);
        }
    }
}
