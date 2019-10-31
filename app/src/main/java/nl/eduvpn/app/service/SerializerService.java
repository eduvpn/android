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

import android.util.Pair;

import net.openid.appauth.AuthState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.InstanceList;
import nl.eduvpn.app.entity.KeyPair;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedAuthState;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.entity.Settings;
import nl.eduvpn.app.entity.message.Maintenance;
import nl.eduvpn.app.entity.message.Message;
import nl.eduvpn.app.entity.message.Notification;
import nl.eduvpn.app.utils.Log;
import nl.eduvpn.app.utils.TTLCache;

/**
 * This service is responsible for (de)serializing objects used in the app.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class SerializerService {

    public class UnknownFormatException extends Exception {
        UnknownFormatException(String message) {
            super(message);
        }

        UnknownFormatException(Throwable throwable) {
            super(throwable);
        }
    }

    private static final String TAG = SerializerService.class.getName();
    private static final DateFormat API_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    static {
        API_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    /**
     * Deserializes a JSON to a list of Profile object.
     *
     * @param json The JSON to deserialize.
     * @return The JSON parsed to a list of Profile instances.
     * @throws UnknownFormatException Thrown if there was a problem while parsing the JSON.
     */
    public List<Profile> deserializeProfileList(JSONObject json) throws UnknownFormatException {
        try {
            if (json.has("profile_list")) {
                JSONObject dataObject = json.getJSONObject("profile_list");
                JSONArray profileList = dataObject.getJSONArray("data");
                List<Profile> result = new ArrayList<>(profileList.length());
                for (int i = 0; i < profileList.length(); ++i) {
                    JSONObject profileObject = profileList.getJSONObject(i);
                    result.add(deserializeProfile(profileObject));
                }
                return result;
            } else {
                throw new UnknownFormatException("'profile_list' key missing!");
            }
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a profile to JSON.
     *
     * @param profile The profile to serialize.
     * @return The profile in a JSON format.
     */
    public JSONObject serializeProfile(Profile profile) throws UnknownFormatException {
        JSONObject result = new JSONObject();
        try {
            result.put("display_name", profile.getDisplayName());
            result.put("profile_id", profile.getProfileId());
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a profile JSON to an object instance.
     *
     * @param jsonObject The JSON to deserialize.
     * @return The profile as a POJO
     * @throws UnknownFormatException Thrown if the format was unknown.
     */
    public Profile deserializeProfile(JSONObject jsonObject) throws UnknownFormatException {
        try {
            String displayName = jsonObject.getString("display_name");
            String profileId = jsonObject.getString("profile_id");
            return new Profile(displayName, profileId);
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a JSON to an InstanceList instance.
     *
     * @param json The JSON to deserialize.
     * @return The JSON in the InstanceList POJO format.
     * @throws UnknownFormatException Thrown if there was a problem while parsing the JSON.
     */
    public InstanceList deserializeInstanceList(JSONObject json) throws UnknownFormatException {
        try {
            Integer sequenceNumber;
            if (json.has("seq")) {
                sequenceNumber = json.getInt("seq");
            } else {
                sequenceNumber = -1; // This will make sure that the new one will surely have a greater number.
            }
            JSONArray instanceArray = json.getJSONArray("instances");
            List<Instance> instances = new ArrayList<>();
            for (int i = 0; i < instanceArray.length(); ++i) {
                JSONObject instanceObject = instanceArray.getJSONObject(i);
                instances.add(deserializeInstance(instanceObject));
            }
            return new InstanceList(instances, sequenceNumber);
        } catch (JSONException ex) {
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
    public JSONObject serializeInstance(Instance instance) throws UnknownFormatException {
        JSONObject result = new JSONObject();
        try {
            result.put("base_uri", instance.getBaseURI());
            result.put("display_name", instance.getDisplayName());
            result.put("logo", instance.getLogoUri());
            result.put("is_custom", instance.isCustom());
            result.put("authorization_type", instance.getAuthorizationType());
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
        return result;
    }

    /**
     * Deserializes an instance object from a JSON.
     *
     * @param jsonObject The JSON object to parse.
     * @return The instance as a POJO.
     * @throws UnknownFormatException Thrown when the format was not as expected.
     */
    public Instance deserializeInstance(JSONObject jsonObject) throws UnknownFormatException {
        try {
            String baseUri = jsonObject.getString("base_uri");
            String displayName;
            if (jsonObject.get("display_name") instanceof String) {
                displayName = jsonObject.getString("display_name");
            } else {
                JSONObject translatedNames = jsonObject.getJSONObject("display_name");
                String userLanguage = _getUserLanguage();
                if (translatedNames.has(userLanguage)) {
                    displayName = translatedNames.getString(userLanguage);
                } else if (translatedNames.has("en-US")) {
                    displayName = translatedNames.getString("en-US");
                } else {
                    String firstKey = translatedNames.keys().next();
                    displayName = translatedNames.getString(firstKey);
                }
            }
            String logoUri = null;
            if (jsonObject.has("logo")) {
                logoUri = jsonObject.getString("logo");
            }
            boolean isCustom = false;
            if (jsonObject.has("is_custom")) {
                isCustom = jsonObject.getBoolean("is_custom");
            }
            @AuthorizationType int authorizationType = AuthorizationType.LOCAL;
            if (jsonObject.has("authorization_type")) {
                //noinspection WrongConstant
                authorizationType = jsonObject.getInt("authorization_type");
            }
            return new Instance(baseUri, displayName, logoUri, authorizationType, isCustom);
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }

    }

    /**
     * Serializes an InstanceList object.
     *
     * @param instanceList The object to serialize.
     * @return The result in a JSON representation.
     * @throws UnknownFormatException Thrown if there was a problem when serializing.
     */
    public JSONObject serializeInstanceList(InstanceList instanceList) throws UnknownFormatException {
        try {
            JSONObject serialized = new JSONObject();
            serialized.put("seq", instanceList.getSequenceNumber());
            JSONArray serializedInstances = new JSONArray();
            for (Instance instance : instanceList.getInstanceList()) {
                JSONObject serializedInstance = serializeInstance(instance);
                serializedInstances.put(serializedInstance);
            }
            serialized.put("instances", serializedInstances);
            return serialized;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a JSON object containing the discovered API endpoints.
     *
     * @param result The JSON object to deserialize
     * @return The discovered API object.
     * @throws UnknownFormatException Thrown if the JSON had an unknown format.
     */
    public DiscoveredAPI deserializeDiscoveredAPI(JSONObject result) throws UnknownFormatException {
        try {
            // we only support version 1 at the moment
            JSONObject apiObject = result.getJSONObject("api");
            if (apiObject == null) {
                throw new UnknownFormatException("'api' is missing!");
            }
            JSONObject apiVersionedObject = apiObject.getJSONObject("http://eduvpn.org/api#2");
            if (apiVersionedObject == null) {
                throw new UnknownFormatException("'http://eduvpn.org/api#2' is missing!");
            }
            String apiBaseUri = apiVersionedObject.getString("api_base_uri");
            if (apiBaseUri == null) {
                throw new UnknownFormatException("'api_base_uri' is missing!");
            }
            String authorizationEndpoint = apiVersionedObject.getString("authorization_endpoint");
            if (authorizationEndpoint == null) {
                throw new UnknownFormatException("'authorization_endpoint' is missing!");
            }
            String tokenEndpoint = apiVersionedObject.getString("token_endpoint");
            if (tokenEndpoint == null) {
                throw new UnknownFormatException("'token_endpoint' is missing!");
            }
            return new DiscoveredAPI(apiBaseUri, authorizationEndpoint, tokenEndpoint);
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a discovered API object.
     *
     * @param discoveredAPI The object to serialize to JSON.
     * @return The object as a JSON.
     * @throws UnknownFormatException Thrown if there was an error while parsing.
     */
    public JSONObject serializeDiscoveredAPI(DiscoveredAPI discoveredAPI) throws UnknownFormatException {
        JSONObject result = new JSONObject();
        try {
            JSONObject apiVersionedObject = new JSONObject();
            apiVersionedObject.put("api_base_uri", discoveredAPI.getApiBaseUri());
            apiVersionedObject.put("authorization_endpoint", discoveredAPI.getAuthorizationEndpoint());
            apiVersionedObject.put("token_endpoint", discoveredAPI.getTokenEndpoint());
            JSONObject apiObject = new JSONObject();
            apiObject.put("http://eduvpn.org/api#2", apiVersionedObject);
            result.put("api", apiObject);
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a JSON with a list of messages into an ArrayList of message object.
     *
     * @param jsonObject    The JSON to deserialize.
     * @param messageSource the message source, either "user_messages" or "system_messages"
     * @return The message instances in a list.
     * @throws UnknownFormatException Thrown if there was a problem while parsing.
     */
    public List<Message> deserializeMessageList(JSONObject jsonObject, String messageSource) throws UnknownFormatException {
        try {
            JSONObject dataObject = jsonObject.getJSONObject(messageSource);
            JSONArray messagesArray = dataObject.getJSONArray("data");
            List<Message> result = new ArrayList<>();
            for (int i = 0; i < messagesArray.length(); ++i) {
                JSONObject messageObject = messagesArray.getJSONObject(i);
                String dateString = messageObject.getString("date_time");
                Date date = API_DATE_FORMAT.parse(dateString);
                String messageType = messageObject.getString("type");
                if ("maintenance".equals(messageType)) {
                    String startString = messageObject.getString("begin");
                    Date startDate = API_DATE_FORMAT.parse(startString);
                    String endString = messageObject.getString("end");
                    Date endDate = API_DATE_FORMAT.parse(endString);
                    result.add(new Maintenance(date, startDate, endDate));
                } else if ("notification".equals(messageType)) {
                    String content = messageObject.getString("message");
                    result.add(new Notification(date, content));
                } else {
                    Log.w(TAG, "Unknown message type: " + messageType);
                }
            }
            return result;
        } catch (JSONException | ParseException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a list of messages into a JSON format.
     *
     * @param messageList   The list of messages to serialize.
     * @param messageSource the message source, either "user_messages" or "system_messages"
     * @return The messages as a JSON object.
     * @throws UnknownFormatException Thrown if there was an error constructing the JSON.
     */
    public JSONObject serializeMessageList(List<Message> messageList, String messageSource) throws UnknownFormatException {
        try {
            JSONObject result = new JSONObject();
            JSONObject dataObject = new JSONObject();
            result.put(messageSource, dataObject);
            JSONArray messagesArray = new JSONArray();
            dataObject.put("data", messagesArray);
            for (Message message : messageList) {
                JSONObject messageObject = new JSONObject();
                messageObject.put("date_time", API_DATE_FORMAT.format(message.getDate()));
                if (message instanceof Maintenance) {
                    messageObject.put("begin", API_DATE_FORMAT.format(((Maintenance)message).getStart()));
                    messageObject.put("end", API_DATE_FORMAT.format(((Maintenance)message).getEnd()));
                    messageObject.put("type", "maintenance");
                } else if (message instanceof Notification) {
                    messageObject.put("message", ((Notification)message).getContent());
                    messageObject.put("type", "notification");
                } else {
                    throw new RuntimeException("Unexpected message format!");
                }
                messagesArray.put(messageObject);
            }
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a list of saved authentication states.
     *
     * @param savedAuthStateList The list with the saved authentication states.
     * @return The parsed list in a JSON format.
     * @throws UnknownFormatException Thrown if there was an unexpected error.
     */
    public JSONObject serializeSavedAuthStateList(List<SavedAuthState> savedAuthStateList) throws UnknownFormatException {
        try {
            JSONObject result = new JSONObject();
            JSONArray array = new JSONArray();
            for (SavedAuthState savedAuthState : savedAuthStateList) {
                JSONObject authStateJson = new JSONObject();
                authStateJson.put("instance", serializeInstance(savedAuthState.getInstance()));
                authStateJson.put("auth_state", savedAuthState.getAuthState().jsonSerializeString());
                array.put(authStateJson);
            }
            result.put("data", array);
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a JSON containing the list of the saved access tokens.
     *
     * @param jsonObject The JSON containing the information.
     * @return The list as a POJO.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public List<SavedAuthState> deserializeSavedAuthStateList(JSONObject jsonObject) throws UnknownFormatException {
        try {
            List<SavedAuthState> result = new ArrayList<>();
            JSONArray dataArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); ++i) {
                JSONObject tokenObject = dataArray.getJSONObject(i);
                Instance instance = deserializeInstance(tokenObject.getJSONObject("instance"));
                String authStateString = tokenObject.getString("auth_state");
                AuthState authState = AuthState.jsonDeserialize(authStateString);
                result.add(new SavedAuthState(instance, authState));
            }
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a list of saved profiles.
     *
     * @param savedProfileList The list of saved profiles.
     * @return The list as a JSON.
     * @throws UnknownFormatException Thrown if there was an error while serializing.
     */
    public JSONObject serializeSavedProfileList(List<SavedProfile> savedProfileList) throws UnknownFormatException {
        try {
            JSONObject result = new JSONObject();
            JSONArray array = new JSONArray();
            for (SavedProfile savedProfile : savedProfileList) {
                JSONObject profileJson = new JSONObject();
                profileJson.put("provider", serializeInstance(savedProfile.getInstance()));
                profileJson.put("profile", serializeProfile(savedProfile.getProfile()));
                profileJson.put("profile_uuid", savedProfile.getProfileUUID());
                array.put(profileJson);
            }
            result.put("data", array);
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a list of saved profiles.
     *
     * @param jsonObject The JSON to deserialize from.
     * @return The list of saved profiles as a POJO.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public List<SavedProfile> deserializeSavedProfileList(JSONObject jsonObject) throws UnknownFormatException {
        try {
            List<SavedProfile> result = new ArrayList<>();
            JSONArray dataArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); ++i) {
                JSONObject profileObject = dataArray.getJSONObject(i);
                Instance instance = deserializeInstance(profileObject.getJSONObject("provider"));
                Profile profile = deserializeProfile(profileObject.getJSONObject("profile"));
                String profileUUID = profileObject.getString("profile_uuid");
                result.add(new SavedProfile(instance, profile, profileUUID));
            }
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a TTL cache of discovered APIs.
     *
     * @param ttlCache The cache to serialize.
     * @return The cache in a JSON format.
     * @throws UnknownFormatException Thrown if there was an error while serializing.
     */
    public JSONObject serializeDiscoveredAPITTLCache(TTLCache<DiscoveredAPI> ttlCache) throws UnknownFormatException {
        try {
            JSONObject result = new JSONObject();
            long purgeAfterSeconds = ttlCache.getPurgeAfterSeconds();
            result.put("purge_after_seconds", purgeAfterSeconds);
            Map<String, Pair<Date, DiscoveredAPI>> originalData = ttlCache.getEntries();
            JSONArray array = new JSONArray();
            result.put("data", array);
            for (Map.Entry<String, Pair<Date, DiscoveredAPI>> entry : originalData.entrySet()) {
                JSONObject entity = new JSONObject();
                entity.put("entry_date", entry.getValue().first.getTime());
                entity.put("key", entry.getKey());
                entity.put("discovered_api", serializeDiscoveredAPI(entry.getValue().second));
                array.put(entity);
            }
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a TTL cache of discovered APIs.
     *
     * @param jsonObject The JSON object to deserialize from.
     * @return The cache object.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public TTLCache<DiscoveredAPI> deserializeDiscoveredAPITTLCache(JSONObject jsonObject) throws UnknownFormatException {
        try {
            Map<String, Pair<Date, DiscoveredAPI>> originalData = new HashMap<>();
            long purgeAfterSeconds = jsonObject.getLong("purge_after_seconds");
            JSONArray dataArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); ++i) {
                JSONObject entity = dataArray.getJSONObject(i);
                Date entryDate = new Date(entity.getLong("entry_date"));
                String key = entity.getString("key");
                DiscoveredAPI discoveredAPI = deserializeDiscoveredAPI(entity.getJSONObject("discovered_api"));
                originalData.put(key, new Pair<>(entryDate, discoveredAPI));
            }
            return new TTLCache<>(originalData, purgeAfterSeconds);
        } catch (JSONException ex) {
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
     * @param jsonObject The json representation of the key pair.
     * @return The keypair instance if succeeded.
     * @throws UnknownFormatException Thrown when the format of the JSON does not match the app format.
     */
    public KeyPair deserializeKeyPair(JSONObject jsonObject) throws UnknownFormatException {
        try {
            JSONObject innerObject = jsonObject.getJSONObject("create_keypair");
            JSONObject dataObject = innerObject.getJSONObject("data");
            String certificate = dataObject.getString("certificate");
            String privateKey = dataObject.getString("private_key");
            boolean isOK = innerObject.getBoolean("ok");
            return new KeyPair(isOK, certificate, privateKey);
        } catch (JSONException ex) {
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
    public JSONObject serializeKeyPair(KeyPair keyPair) throws UnknownFormatException {
        JSONObject result = new JSONObject();
        JSONObject innerObject = new JSONObject();
        JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("certificate", keyPair.getCertificate());
            dataObject.put("private_key", keyPair.getPrivateKey());
            innerObject.put("ok", keyPair.isOK());
            innerObject.put("data", dataObject);
            result.put("create_keypair", innerObject);
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Serializes a saved key pair.
     *
     * @param savedKeyPair The saved key pair to serialize.
     * @return The JSON representation of the saved key pair.
     * @throws UnknownFormatException Thrown if an error happens during serialization.
     */
    public JSONObject serializeSavedKeyPair(SavedKeyPair savedKeyPair) throws UnknownFormatException {
        JSONObject result = new JSONObject();
        try {
            result.put("instance", serializeInstance(savedKeyPair.getInstance()));
            result.put("key_pair", serializeKeyPair(savedKeyPair.getKeyPair()));
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Deserializes a key pair from JSON.
     *
     * @param jsonObject The JSON object to deserialize.
     * @return The saved key pair instance.
     * @throws UnknownFormatException Thrown if the JSON has an unknown format.
     */
    public SavedKeyPair deserializeSavedKeyPair(JSONObject jsonObject) throws UnknownFormatException {
        try {
            Instance instance = deserializeInstance(jsonObject.getJSONObject("instance"));
            KeyPair keyPair = deserializeKeyPair(jsonObject.getJSONObject("key_pair"));
            return new SavedKeyPair(instance, keyPair);
        } catch (JSONException ex) {
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
    public JSONObject serializeSavedKeyPairList(List<SavedKeyPair> savedKeyPairList) throws UnknownFormatException {
        try {
            JSONArray serialized = new JSONArray();
            for (SavedKeyPair savedKeyPair : savedKeyPairList) {
                JSONObject serializedKeyPair = serializeSavedKeyPair(savedKeyPair);
                serialized.put(serializedKeyPair);
            }
            JSONObject result = new JSONObject();
            result.put("items", serialized);
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }


    /**
     * Deserializes a list of saved key pairs.
     *
     * @param jsonObject The json to deserialize from.
     * @return The list of saved key pairs created from the JSON.
     * @throws UnknownFormatException Thrown if there was an error while deserializing.
     */
    public List<SavedKeyPair> deserializeSavedKeyPairList(JSONObject jsonObject) throws UnknownFormatException {
        try {
            List<SavedKeyPair> result = new ArrayList<>();
            JSONArray itemsList = jsonObject.getJSONArray("items");
            for (int i = 0; i < itemsList.length(); ++i) {
                JSONObject serializedItem = itemsList.getJSONObject(i);
                SavedKeyPair savedKeyPair = deserializeSavedKeyPair(serializedItem);
                result.add(savedKeyPair);
            }
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }


    /**
     * Returns the language of the user in a specific "country-language" format.

     * @return The current language of the user.
     */
    private String _getUserLanguage() {
        // Converts 'en_US' to 'en-US'
        return Locale.getDefault().toString().replace('_', '-');
    }
}
