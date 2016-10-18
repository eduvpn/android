package net.tuxed.vpnconfigimporter.service;

import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.InstanceList;
import net.tuxed.vpnconfigimporter.entity.Profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This service is responsible for (de)serializing objects used in the app.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class SerializerService {

    public class UnknownFormatException extends Exception {
        public UnknownFormatException(String message) {
            super(message);
        }

        public UnknownFormatException(Throwable throwable) {
            super(throwable);
        }
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
            if (json.has("data")) {
                JSONObject dataObject = json.getJSONObject("data");
                if (dataObject.has("profile_list")) {
                    JSONArray poolList = dataObject.getJSONArray("profile_list");
                    List<Profile> result = new ArrayList<>(poolList.length());
                    for (int i = 0; i < poolList.length(); ++i) {
                        JSONObject profileObject = poolList.getJSONObject(i);
                        result.add(deserializeProfile(profileObject));
                    }
                    return result;
                } else {
                    throw new UnknownFormatException("'profile_list' key inside 'data' missing!");
                }
            } else {
                throw new UnknownFormatException("'data' key missing!");
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
            result.put("pool_id", profile.getPoolId());
            result.put("two_factor", profile.getTwoFactor());
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
            String poolId = jsonObject.getString("pool_id");
            Boolean twoFactor = jsonObject.getBoolean("two_factor");
            return new Profile(displayName, poolId, twoFactor);
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
            Integer listVersion = json.getInt("list_version");
            if (listVersion != 1) {
                throw new UnknownFormatException("Unknown list_version property: " + listVersion);
            }
            JSONArray instanceArray = json.getJSONArray("instances");
            List<Instance> instances = new ArrayList<>();
            for (int i = 0; i < instanceArray.length(); ++i) {
                JSONObject instanceObject = instanceArray.getJSONObject(i);
                instances.add(deserializeInstance(instanceObject));
            }
            return new InstanceList(listVersion, instances);
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
            result.put("base_uri", instance.getBaseUri());
            result.put("display_name", instance.getDisplayName());
            result.put("logo_uri", instance.getLogoUri());
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
            String displayName = jsonObject.getString("display_name");
            String logoUri = null;
            if (jsonObject.has("logo_uri")) {
                logoUri = jsonObject.getString("logo_uri");
            }
            return new Instance(baseUri, displayName, logoUri);
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
            serialized.put("list_version", instanceList.getVersion());
            JSONArray serializedInstances = new JSONArray();
            for (Instance instance : instanceList.getInstanceList()) {
                JSONObject serializedInstance = new JSONObject();
                serializedInstance.put("base_uri", instance.getBaseUri());
                serializedInstance.put("display_name", instance.getDisplayName());
                serializedInstance.put("logo_uri", instance.getLogoUri());
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
            Integer apiVersion = result.getInt("api_version");
            if (apiVersion != 1) {
                throw new UnknownFormatException("Unknown API version: " + apiVersion);
            }
            String authorizationEndpoint = result.getString("authorization_endpoint");
            if (authorizationEndpoint == null) {
                throw new UnknownFormatException("'authorization_endpoint' is missing!");
            }
            JSONObject apiObject = result.getJSONObject("api");
            String createConfigAPI = apiObject.getString("create_config");
            if (createConfigAPI == null) {
                throw new UnknownFormatException("'create_config' is missing!");
            }
            String profileListAPI = apiObject.getString("profile_list");
            if (profileListAPI == null) {
                throw new UnknownFormatException("'profile_list' is missing!");
            }
            String systemMessagesAPI = null;
            if (apiObject.has("system_messages")) {
                systemMessagesAPI = apiObject.getString("system_messages");
            }
            String userMessagesAPI = null;
            if (apiObject.has("user_messages")) {
                userMessagesAPI = apiObject.getString("user_messages");
            }
            return new DiscoveredAPI(apiVersion, authorizationEndpoint, createConfigAPI,
                    profileListAPI, systemMessagesAPI, userMessagesAPI);
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
            result.put("api_version", discoveredAPI.getApiVersion());
            result.put("authorization_endpoint", discoveredAPI.getAuthorizationEndpoint());
            JSONObject apiObject = new JSONObject();
            apiObject.put("create_config", discoveredAPI.getCreateConfigAPI());
            apiObject.put("profile_list", discoveredAPI.getProfileListAPI());
            apiObject.put("user_messages", discoveredAPI.getUserMessagesAPI());
            apiObject.put("system_messages", discoveredAPI.getSystemMessagesAPI());
            result.put("api", apiObject);
            return result;
        } catch (JSONException ex) {
            throw new UnknownFormatException(ex);
        }
    }
}
