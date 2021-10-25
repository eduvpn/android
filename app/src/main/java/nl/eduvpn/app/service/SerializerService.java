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

import androidx.annotation.NonNull;

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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import nl.eduvpn.app.Constants;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.InstanceList;
import nl.eduvpn.app.entity.KeyPair;
import nl.eduvpn.app.entity.Organization;
import nl.eduvpn.app.entity.OrganizationList;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedAuthState;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.entity.ServerList;
import nl.eduvpn.app.entity.Settings;
import nl.eduvpn.app.entity.TranslatableString;
import nl.eduvpn.app.entity.message.Maintenance;
import nl.eduvpn.app.entity.message.Message;
import nl.eduvpn.app.entity.message.Notification;
import nl.eduvpn.app.utils.Log;

/**
 * This service is responsible for (de)serializing objects used in the app.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class SerializerService {

    public static class UnknownFormatException extends Exception {
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
     * Serialized a list of profiles to JSON formater
     *
     * @param profileList The list of profiles to serialize.
     * @return The list of profiles in a JSON array.
     * @throws UnknownFormatException Thrown if there was a problem while creating the JSON.
     */
    public JSONObject serializeProfileList(@NonNull List<Profile> profileList) throws UnknownFormatException {
        JSONObject result = new JSONObject();
        JSONObject data = new JSONObject();
        JSONArray array = new JSONArray();
        for (Profile profile : profileList) {
            JSONObject profileObject = serializeProfile(profile);
            array.put(profileObject);
        }
        try {
            result.put("profile_list", data);
            data.put("data", array);
        } catch (JSONException ex) {
            throw new UnknownFormatException("Unable to create nested object for serialized profile list!");
        }
        return result;
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
            int sequenceNumber;
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
     * Deserializes a JSON to an InstanceList instance.
     *
     * @param instanceArray The JSON array to deserialize.
     * @return The JSON as a list of instances.
     * @throws UnknownFormatException Thrown if there was a problem while parsing the JSON.
     */
    public List<Instance> deserializeInstances(JSONArray instanceArray) throws UnknownFormatException {
        try {
            List<Instance> instances = new ArrayList<>();
            for (int i = 0; i < instanceArray.length(); ++i) {
                JSONObject instanceObject = instanceArray.getJSONObject(i);
                instances.add(deserializeInstance(instanceObject));
            }
            return instances;
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
            result.put("base_url", instance.getBaseURI());
            if (instance.getDisplayName() != null) {
                result.put("display_name", instance.getDisplayName());
            }
            result.put("logo", instance.getLogoUri());
            result.put("is_custom", instance.isCustom());
            String authType;
            if (instance.getAuthorizationType() == AuthorizationType.Local) {
                authType = "institute_access";
            } else if (instance.getAuthorizationType() == AuthorizationType.Distributed) {
                authType = "secure_internet";
            } else {
                authType = "organization";
            }
            result.put("server_type", authType);
            JSONArray supportContact = new JSONArray();
            for (String contact : instance.getSupportContact()) {
                supportContact.put(contact);
            }
            if (instance.getCountryCode() != null) {
                result.put("country_code", instance.getCountryCode());
            }
            if (instance.getAuthenticationUrlTemplate() != null) {
                result.put("authentication_url_template", instance.getAuthenticationUrlTemplate());
            }
            result.put("support_contact", supportContact);
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
            // New version: base_url, old one: base_uri
            String baseUri;
            if (jsonObject.has("base_url")) {
                baseUri = jsonObject.getString("base_url");
            } else {
                baseUri = jsonObject.getString("base_uri");
            }
            String displayName = null;
            if (jsonObject.has("display_name")) {
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
            }
            String logoUri = null;
            if (jsonObject.has("logo")) {
                logoUri = jsonObject.getString("logo");
            }
            boolean isCustom = false;
            if (jsonObject.has("is_custom")) {
                isCustom = jsonObject.getBoolean("is_custom");
            }
            AuthorizationType authorizationType = AuthorizationType.Local;

            if (jsonObject.has("server_type")) {
                // New version
                String authorizationTypeString = jsonObject.getString("server_type");
                if ("secure_internet".equals(authorizationTypeString)) {
                    authorizationType = AuthorizationType.Distributed;
                } else if ("institute_access".equals(authorizationTypeString)) {
                    //noinspection ConstantConditions
                    authorizationType = AuthorizationType.Local;
                } else if ("organization".equals(authorizationTypeString)) {
                    authorizationType = AuthorizationType.Organization;
                }
            } else if (jsonObject.has("authorization_type")) {
                // Old version
                int authorizationTypeInt = jsonObject.getInt("authorization_type");
                if (authorizationTypeInt == 1) {
                    authorizationType = AuthorizationType.Distributed;
                } else if (authorizationTypeInt == 2) {
                    authorizationType = AuthorizationType.Organization;
                }
            }
            List<String> supportContact = new ArrayList<>();
            if (jsonObject.has("support_contact")) {
                JSONArray supportArray = jsonObject.getJSONArray("support_contact");
                for (int supportIndex = 0; supportIndex < supportArray.length(); ++supportIndex) {
                    supportContact.add(supportArray.getString(supportIndex));
                }
            }
            String countryCode = null;
            if (jsonObject.has("country_code")) {
                countryCode = jsonObject.getString("country_code");
            }
            String authenticationUrlTemplate = null;
            if (jsonObject.has("authentication_url_template")) {
                authenticationUrlTemplate = jsonObject.getString("authentication_url_template");
            }
            return new Instance(baseUri, displayName, logoUri, authorizationType, countryCode, isCustom, authenticationUrlTemplate, supportContact);
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
     * Serializes a list of instances.
     *
     * @param instanceList The object to serialize.
     * @return The result in a JSON representation.
     * @throws UnknownFormatException Thrown if there was a problem when serializing.
     */
    public JSONArray serializeInstances(List<Instance> instanceList) throws UnknownFormatException {
        JSONArray serializedInstances = new JSONArray();
        for (Instance instance : instanceList) {
            JSONObject serializedInstance = serializeInstance(instance);
            serializedInstances.put(serializedInstance);
        }
        return serializedInstances;
    }

    /**
     * Deserializes a JSON object containing the discovered API endpoints.
     *
     * @param result The JSON object to deserialize
     * @return The discovered API object.
     * @throws UnknownFormatException Thrown if the JSON had an unknown format.
     */
    @NonNull
    public DiscoveredAPI deserializeDiscoveredAPI(JSONObject result) throws UnknownFormatException {
        try {
            JSONObject apiObject = result.optJSONObject("api");
            if (apiObject == null) {
                throw new UnknownFormatException("'api' is missing!");
            }
            JSONObject apiVersionedObject = apiObject.optJSONObject("http://eduvpn.org/api#2");
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
     * Serializes a list of saved authorization states.
     *
     * @param savedAuthStateList The list with the saved authorization states.
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
    public ServerList deserializeServerList(JSONObject jsonObject) throws UnknownFormatException {
        try {
            List<Instance> result = new ArrayList<>();
            JSONArray itemsList = jsonObject.getJSONArray("server_list");
            long version = jsonObject.getLong("v");
            for (int i = 0; i < itemsList.length(); ++i) {
                JSONObject serializedItem = itemsList.getJSONObject(i);
                Instance instance = deserializeInstance(serializedItem);
                result.add(instance);
            }
            return new ServerList(version, result);
        } catch (JSONException ex) {
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
    public JSONObject serializeServerList(ServerList serverList) throws UnknownFormatException {
        try {
            JSONObject output = new JSONObject();
            JSONArray itemsList = new JSONArray();

            output.put("server_list", itemsList);
            output.put("v", serverList.getVersion());
            for (int i = 0; i < serverList.getServerList().size(); ++i) {
                JSONObject serializedItem = serializeInstance(serverList.getServerList().get(i));
                itemsList.put(serializedItem);
            }
            return output;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }

    /**
     * Returns the language of the user in a specific "country-language" format.
     *
     * @return The current language of the user.
     */
    private String _getUserLanguage() {
        // Converts 'en_US' to 'en-US'
        return Constants.LOCALE.toString().replace('_', '-');
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
}
