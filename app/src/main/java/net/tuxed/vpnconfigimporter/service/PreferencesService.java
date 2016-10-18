package net.tuxed.vpnconfigimporter.service;

import android.content.Context;
import android.content.SharedPreferences;

import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.utils.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
    private SharedPreferences _getSharedPreferences() {
        return _context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves the connection state.
     *
     * @param state The state to save.
     */
    public void saveConnectionState(String state) {
        _getSharedPreferences().edit()
                .putString(KEY_STATE, state)
                .apply();
    }

    /**
     * Returns the lastly saved connection state string.
     *
     * @return The saved connection state. Null if none.
     */
    public String getConnectionState() {
        return _getSharedPreferences().getString(KEY_STATE, null);
    }

    /**
     * Removed the saved state, if any.
     */
    public void removeSavedConnectionState() {
        _getSharedPreferences().edit().remove(KEY_STATE).apply();
    }

    /**
     * Saves the instance the app will connect to.
     *
     * @param instance The instance to save.
     */
    public void saveConnectionInstance(Instance instance) {
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
    public Instance getSavedInstance() {
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
    public void saveProfile(Profile profile) {
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
     * @return The lastly saved profile with {@link #saveProfile(Profile)}.
     */
    public Profile getSavedProfile() {
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
    public void saveAccessToken(String accessToken) {
        _getSharedPreferences().edit().putString(KEY_ACCESS_TOKEN, accessToken).apply();
    }

    /**
     * Returns a saved access token, if any found.
     *
     * @return The lastly saved access token.
     */
    public String getAccessToken() {
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
    public void saveDiscoveredAPI(DiscoveredAPI discoveredAPI) {
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
    public DiscoveredAPI getSavedDiscoveredAPI() {
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
}
