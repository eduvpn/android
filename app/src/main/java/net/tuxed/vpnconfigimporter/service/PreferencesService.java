package net.tuxed.vpnconfigimporter.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.entity.SavedProfile;
import net.tuxed.vpnconfigimporter.entity.SavedToken;
import net.tuxed.vpnconfigimporter.utils.Log;
import net.tuxed.vpnconfigimporter.utils.TTLCache;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * This service is used to save temporary data
 * Created by Daniel Zolnai on 2016-10-11.
 */

public class PreferencesService {
    private static final String TAG = PreferencesService.class.getName();

    private static final String PREFERENCES_NAME = "preferences_service";

    private static final String KEY_STATE = "state";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_CUSTOM_TABS_OPT_OUT = "custom_tabs_opt_out";

    private static final String KEY_INSTANCE = "instance";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_DISCOVERED_API = "discovered_api";

    private static final String KEY_SAVED_PROFILES = "saved_profiles";
    private static final String KEY_SAVED_TOKENS = "saved_tokens";
    private static final String KEY_DISCOVERED_API_CACHE = "discovered_api_cache";

    private static final String KEY_AUTOCONNECT_UUID = "autoconnect_uuid";

    private Context _context;
    private SerializerService _serializerService;

    /**
     * Constructor.
     *
     * @param context           The application or activity context.
     * @param serializerService The serializer service used to serialize and deserialize objects.
     */
    public PreferencesService(Context context, SerializerService serializerService) {
        _context = context;
        _serializerService = serializerService;
    }

    /**
     * Returns the shared preferences to be used throughout this service.
     *
     * @return The preferences to be used.
     */
    SharedPreferences _getSharedPreferences() {
        return _context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves the connection state.
     *
     * @param state The state to save.
     */
    public void currentConnectionState(@NonNull String state) {
        _getSharedPreferences().edit()
                .putString(KEY_STATE, state)
                .apply();
    }

    /**
     * Returns the lastly saved connection state string.
     *
     * @return The saved connection state. Null if none.
     */
    public String getCurrentConnectionState() {
        return _getSharedPreferences().getString(KEY_STATE, null);
    }

    /**
     * Removed the saved state, if any.
     */
    public void removeCurrentConnectionState() {
        _getSharedPreferences().edit().remove(KEY_STATE).apply();
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
    public void currentProfile(@NonNull Profile profile) {
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
     * @return The lastly saved profile with {@link #currentProfile(Profile)}.
     */
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
     * Saves the access token for further usage.
     *
     * @param accessToken The access token to use for the VPN provider API.
     */
    public void currentAccessToken(@NonNull String accessToken) {
        _getSharedPreferences().edit().putString(KEY_ACCESS_TOKEN, accessToken).apply();
    }

    /**
     * Returns a saved access token, if any found.
     *
     * @return The lastly saved access token.
     */
    public String getCurrentAccessToken() {
        return _getSharedPreferences().getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Returns if the user has opted out of custom tabs usage.
     *
     * @return True if the user does not want to use Custom Tabs. Otherwise false.
     */
    public boolean getCustomTabsOptOut() {
        return _getSharedPreferences().getBoolean(KEY_CUSTOM_TABS_OPT_OUT, false);
    }

    /**
     * Saves a discovered API object for future usage.
     *
     * @param discoveredAPI The discovered API.
     */
    public void currentDiscoveredAPI(@NonNull DiscoveredAPI discoveredAPI) {
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
     * Returns a previously saved list of saved tokens.
     *
     * @return The saved list, or null if not exists.
     */
    public List<SavedToken> getSavedTokenList() {
        String serializedSavedTokenList = _getSharedPreferences().getString(KEY_SAVED_TOKENS, null);
        if (serializedSavedTokenList == null) {
            return null;
        }
        try {
            return _serializerService.deserializeSavedTokenList(new JSONObject(serializedSavedTokenList));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize saved token list", ex);
            return null;
        }
    }

    /**
     * Stores a saved token list.
     *
     * @param savedTokenList The list to save.
     */
    public void storeSavedTokenList(@NonNull List<SavedToken> savedTokenList) {
        try {
            String serializedSavedTokenList = _serializerService.serializeSavedTokenList(savedTokenList).toString();
            _getSharedPreferences().edit().putString(KEY_SAVED_TOKENS, serializedSavedTokenList).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Can not save saved token list.", ex);
        }
    }

    /**
     * Retrieves a saved TTL cache of discovered APIs.
     *
     * @return The discovered API cache. Null if no saved one.
     */
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
     * Stores the VPN profile UUID the app should connect to when the login has happened.
     * Only set this when the user has clicked on a profile to connect, and has to login.
     * So after the login he gets to the connection screen immediately.
     *
     * @param uuid The UUID of the saved profile.
     */
    public void storeAutoConnectUUID(@NonNull String uuid) {
        _getSharedPreferences().edit().putString(KEY_AUTOCONNECT_UUID, uuid).apply();
    }

    /**
     * Removes the auto connect profile UUID.
     */
    public void removeAutoConnectUUID() {
        _getSharedPreferences().edit().remove(KEY_AUTOCONNECT_UUID).apply();
    }

    /**
     * Returns the auto connect profile UUID key.
     *
     * @return The UUID of the profile the app should connect to after the login immediately.
     * Do not forget to remove this, otherwise there will be some unexpected connections :)
     */
    public String getAutoConnectUUID() {
        return _getSharedPreferences().getString(KEY_AUTOCONNECT_UUID, null);
    }
}
