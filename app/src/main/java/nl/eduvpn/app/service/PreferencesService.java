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

import android.content.SharedPreferences;

import net.openid.appauth.AuthState;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.InstanceList;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.entity.SavedAuthState;
import nl.eduvpn.app.entity.Settings;
import nl.eduvpn.app.utils.Log;
import nl.eduvpn.app.utils.TTLCache;

/**
 * This service is used to save temporary data
 * Created by Daniel Zolnai on 2016-10-11.
 */

public class PreferencesService {
    private static final String TAG = PreferencesService.class.getName();

    private static final String KEY_AUTH_STATE = "auth_state";
    private static final String KEY_APP_SETTINGS = "app_settings";

    private static final String KEY_INSTANCE = "instance";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_DISCOVERED_API = "discovered_api";

    private static final String KEY_INSTANCE_LIST_PREFIX = "instance_list_";
    private static final String KEY_INSTANCE_LIST_SECURE_INTERNET = KEY_INSTANCE_LIST_PREFIX + "secure_internet";
    private static final String KEY_INSTANCE_LIST_INSTITUTE_ACCESS = KEY_INSTANCE_LIST_PREFIX + "institute_access";

    private static final String KEY_SAVED_PROFILES = "saved_profiles";
    private static final String KEY_SAVED_AUTH_STATES = "saved_auth_state";
    private static final String KEY_DISCOVERED_API_CACHE = "discovered_api_cache";
    private static final String KEY_SAVED_KEY_PAIRS = "saved_key_pairs";

    private SerializerService _serializerService;
    private SharedPreferences _sharedPreferences;

    /**
     * Constructor.
     *
     * @param serializerService The serializer service used to serialize and deserialize objects.
     * @param sharedPreferences The secured shared preferences where the data will be stored.
     */
    public PreferencesService(SerializerService serializerService, SharedPreferences sharedPreferences) {
        _sharedPreferences = sharedPreferences;
        _serializerService = serializerService;
    }

    /**
     * Returns the shared preferences to be used throughout this service.
     *
     * @return The preferences to be used.
     */
    SharedPreferences _getSharedPreferences() {
        return _sharedPreferences;
    }

    /**
     * Saves the instance the app will connect to.
     *
     * @param instance The instance to save.
     */
    public void currentInstance(@NonNull Instance instance) {
        try {
            _getSharedPreferences().edit()
                    .putString(KEY_INSTANCE, _serializerService.serializeInstance(instance).toString())
                    .apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Can not save connection instance!", ex);
        }
    }

    /**
     * Returns a saved instance.
     *
     * @return The instance to connect to. Null if none found.
     */
    public Instance getCurrentInstance() {
        String serializedInstance = _getSharedPreferences().getString(KEY_INSTANCE, null);
        if (serializedInstance == null) {
            return null;
        }
        try {
            return _serializerService.deserializeInstance(new JSONObject(serializedInstance));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize instance!", ex);
            return null;
        }
    }

    /**
     * Saves the current profile as the selected one.
     *
     * @param profile The profile to save.
     */
    public void storeCurrentProfile(@NonNull Profile profile) {
        try {
            _getSharedPreferences().edit()
                    .putString(KEY_PROFILE, _serializerService.serializeProfile(profile).toString())
                    .apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Unable to serialize profile!", ex);
        }
    }

    /**
     * Returns the previously saved profile.
     *
     * @return The lastly saved profile with {@link #storeCurrentProfile(Profile)}.
     */
    @Nullable
    public Profile getCurrentProfile() {
        String serializedProfile = _getSharedPreferences().getString(KEY_PROFILE, null);
        if (serializedProfile == null) {
            return null;
        }
        try {
            return _serializerService.deserializeProfile(new JSONObject(serializedProfile));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize saved profile!", ex);
            return null;
        }
    }

    /**
     * Saves the authentication state for further usage.
     *
     * @param authState The access token and refresh token to use for the VPN provider API.
     */
    public void storeCurrentAuthState(@NonNull AuthState authState) {
        _getSharedPreferences().edit().putString(KEY_AUTH_STATE, authState.jsonSerializeString()).apply();
    }

    /**
     * Returns a saved access token, if any found.
     *
     * @return The lastly saved access token.
     */
    @Nullable
    public AuthState getCurrentAuthState() {
        if (!_getSharedPreferences().contains(KEY_AUTH_STATE)) {
            return null;
        }
        try {
            //noinspection ConstantConditions
            return AuthState.jsonDeserialize(_getSharedPreferences().getString(KEY_AUTH_STATE, null));
        } catch (JSONException ex) {
            Log.e(TAG, "Could not deserialize saved authentication state", ex);
            return null;
        }
    }

    /**
     * Returns the saved app settings, or the default settings if none found.
     *
     * @return True if the user does not want to use Custom Tabs. Otherwise false.
     */
    @NonNull
    public Settings getAppSettings() {
        Settings defaultSettings = new Settings(Settings.USE_CUSTOM_TABS_DEFAULT_VALUE, Settings.FORCE_TCP_DEFAULT_VALUE);
        String serializedSettings = _getSharedPreferences().getString(KEY_APP_SETTINGS, null);
        if (serializedSettings == null) {
            // Default settings.
            storeAppSettings(defaultSettings);
            return defaultSettings;
        } else {
            try {
                return _serializerService.deserializeAppSettings(new JSONObject(serializedSettings));
            } catch (SerializerService.UnknownFormatException | JSONException ex) {
                Log.e(TAG, "Unable to deserialize app settings!", ex);
                storeAppSettings(defaultSettings);
                return defaultSettings;
            }
        }
    }

    /**
     * Saves the app settings.
     *
     * @param settings The settings of the app to save.
     */
    public void storeAppSettings(@NonNull Settings settings) {
        try {
            String serializedSettings = _serializerService.serializeAppSettings(settings).toString();
            _getSharedPreferences().edit().putString(KEY_APP_SETTINGS, serializedSettings).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Unable to serialize and save app settings!");
        }

    }

    /**
     * Saves a discovered API object for future usage.
     *
     * @param discoveredAPI The discovered API.
     */
    public void storeCurrentDiscoveredAPI(@NonNull DiscoveredAPI discoveredAPI) {
        try {
            _getSharedPreferences().edit()
                    .putString(KEY_DISCOVERED_API, _serializerService.serializeDiscoveredAPI(discoveredAPI).toString())
                    .apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Can not save discovered API!", ex);
        }
    }

    /**
     * Returns a previously saved discovered API.
     *
     * @return A discovered API if saved, otherwise null.
     */
    @Nullable
    public DiscoveredAPI getCurrentDiscoveredAPI() {
        String serializedDiscoveredAPI = _getSharedPreferences().getString(KEY_DISCOVERED_API, null);
        if (serializedDiscoveredAPI == null) {
            return null;
        }
        try {
            return _serializerService.deserializeDiscoveredAPI(new JSONObject(serializedDiscoveredAPI));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize saved discovered API", ex);
            return null;
        }
    }

    /**
     * Returns a previously saved list of saved profiles.
     *
     * @return The saved list, or null if not exists.
     */
    @Nullable
    public List<SavedProfile> getSavedProfileList() {
        String serializedSavedProfileList = _getSharedPreferences().getString(KEY_SAVED_PROFILES, null);
        if (serializedSavedProfileList == null) {
            return null;
        }
        try {
            return _serializerService.deserializeSavedProfileList(new JSONObject(serializedSavedProfileList));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize saved profile list", ex);
            return null;
        }
    }

    /**
     * Stores a saved profile list.
     *
     * @param savedProfileList The list to save.
     */
    public void storeSavedProfileList(@NonNull List<SavedProfile> savedProfileList) {
        try {
            String serializedSavedProfileList = _serializerService.serializeSavedProfileList(savedProfileList).toString();
            _getSharedPreferences().edit().putString(KEY_SAVED_PROFILES, serializedSavedProfileList).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Can not save saved profile list.", ex);
        }
    }


    /**
     * Returns a previously saved list of saved authentication states.
     *
     * @return The saved list, or null if not exists.
     */
    @Nullable
    public List<SavedAuthState> getSavedAuthStateList() {
        String serializedSavedAuthStateList = _getSharedPreferences().getString(KEY_SAVED_AUTH_STATES, null);
        if (serializedSavedAuthStateList == null) {
            return null;
        }
        try {
            return _serializerService.deserializeSavedAuthStateList(new JSONObject(serializedSavedAuthStateList));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize saved auth state list", ex);
            return null;
        }
    }

    /**
     * Stores a saved token list.
     *
     * @param savedAuthStateList The list to save.
     */
    public void storeSavedAuthStateList(@NonNull List<SavedAuthState> savedAuthStateList) {
        try {
            String serializedSavedAuthStateList = _serializerService.serializeSavedAuthStateList(savedAuthStateList).toString();
            _getSharedPreferences().edit().putString(KEY_SAVED_AUTH_STATES, serializedSavedAuthStateList).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Can not save saved token list.", ex);
        }
    }

    /**
     * Retrieves a saved TTL cache of discovered APIs.
     *
     * @return The discovered API cache. Null if no saved one.
     */
    @Nullable
    public TTLCache<DiscoveredAPI> getDiscoveredAPICache() {
        String serializedCache = _getSharedPreferences().getString(KEY_DISCOVERED_API_CACHE, null);
        if (serializedCache == null) {
            return null;
        }
        try {
            return _serializerService.deserializeDiscoveredAPITTLCache(new JSONObject(serializedCache));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize saved discovered API cache.");
            return null;
        }

    }

    /**
     * Stores the discovered API cache.
     *
     * @param ttlCache The cache to save.
     */
    public void storeDiscoveredAPICache(@NonNull TTLCache<DiscoveredAPI> ttlCache) {
        try {
            String serializedCache = _serializerService.serializeDiscoveredAPITTLCache(ttlCache).toString();
            _getSharedPreferences().edit().putString(KEY_DISCOVERED_API_CACHE, serializedCache).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Can not save discovered API cache.", ex);
        }
    }

    /**
     * Stores the instance list for a specific connection type
     */
    public void storeInstanceList(@AuthorizationType int authorizationType, InstanceList instanceListToSave) {
        String key;
        if (authorizationType == AuthorizationType.DISTRIBUTED) {
            key = KEY_INSTANCE_LIST_INSTITUTE_ACCESS;
        } else if (authorizationType == AuthorizationType.LOCAL) {
            key = KEY_INSTANCE_LIST_SECURE_INTERNET;
        } else {
            throw new RuntimeException("Unexpected connection type!");
        }
        try {
            String serializedInstanceList = _serializerService.serializeInstanceList(instanceListToSave).toString();
            _getSharedPreferences().edit().putString(key, serializedInstanceList).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Cannot save instance list for connection type: " + authorizationType, ex);
        }
    }

    @Nullable
    public List<SavedKeyPair> getSavedKeyPairList() {
        try {
            String serializedKeyPairList = _getSharedPreferences().getString(KEY_SAVED_KEY_PAIRS, null);
            if (serializedKeyPairList == null) {
                return null;
            }
            return _serializerService.deserializeSavedKeyPairList(new JSONObject(serializedKeyPairList));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Cannot retrieve saved key pair list.", ex);
            return null;
        }
    }

    /**
     * Stores the instance list for a specific authorization type
     */
    @Nullable
    public InstanceList getInstanceList(@AuthorizationType int authorizationType) {
        String key;
        if (authorizationType == AuthorizationType.DISTRIBUTED) {
            key = KEY_INSTANCE_LIST_INSTITUTE_ACCESS;
        } else if (authorizationType == AuthorizationType.LOCAL) {
            key = KEY_INSTANCE_LIST_SECURE_INTERNET;
        } else {
            throw new RuntimeException("Unexpected connection type!");
        }
        try {
            String serializedInstanceList = _getSharedPreferences().getString(key, null);
            if (serializedInstanceList == null) {
                return null;
            }
            return _serializerService.deserializeInstanceList(new JSONObject(serializedInstanceList));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Cannot deserialize previously saved instance list of authorization type: " + authorizationType, ex);
            return null;
        }
    }

    /*
     * Stores the list of saved key pairs.
     * @param savedKeyPairs The list of saved key pairs to store.
     */
    public void storeSavedKeyPairList(@NonNull List<SavedKeyPair> savedKeyPairs) {
        try {
            String serializedKeyPairList = _serializerService.serializeSavedKeyPairList(savedKeyPairs).toString();
            _getSharedPreferences().edit().putString(KEY_SAVED_KEY_PAIRS, serializedKeyPairList).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Cannot store saved key pair list.", ex);
        }
    }
}
