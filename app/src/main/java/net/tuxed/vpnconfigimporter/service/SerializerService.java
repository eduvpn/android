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
                        String displayName = profileObject.getString("display_name");
                        String poolId = profileObject.getString("pool_id");
                        Boolean twoFactor = profileObject.getBoolean("two_factor");
                        result.add(new Profile(displayName, poolId, twoFactor));
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
                String baseUri = instanceObject.getString("base_uri");
                String displayName = instanceObject.getString("display_name");
                String logoUri = null;
                if (instanceObject.has("logo_uri")) {
                    logoUri = instanceObject.getString("logo_uri");
                }
                instances.add(new Instance(baseUri, displayName, logoUri));
            }
            return new InstanceList(listVersion, instances);
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
}
