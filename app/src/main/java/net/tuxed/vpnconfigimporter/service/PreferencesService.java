package net.tuxed.vpnconfigimporter.service;

import android.content.Context;
import android.content.SharedPreferences;

import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;

/**
 * This service is used to save temporary data
 * Created by Daniel Zolnai on 2016-10-11.
 */

public class PreferencesService {

    private static final String PREFERENCES_NAME = "preferences_service";

    private static final String KEY_STATE = "state";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_CUSTOM_TABS_OPT_OUT = "custom_tabs_opt_out";

    private static final String KEY_INSTANCE_DISPLAY_NAME = "instance_display_name";
    private static final String KEY_INSTANCE_BASE_URI = "instance_base_uri";
    private static final String KEY_INSTANCE_LOGO_URI = "instance_logo_uri";

    private static final String KEY_PROFILE_POOL_ID = "profile_pool_id";
    private static final String KEY_PROFILE_DISPLAY_NAME = "profile_display_name";
    private static final String KEY_PROFILE_TWO_FACTOR = "profile_two_factor";

    private static final String KEY_DISCOVERED_API_VERSION = "discovered_api_version";
    private static final String KEY_DISCOVERED_API_CREATE_CONFIG_API = "discovered_api_create_config_api";
    private static final String KEY_DISCOVERED_API_PROFILE_LIST_API = "discovered_api_profile_list_api";
    private static final String KEY_DISCOVERED_API_SYSTEM_MESSAGES_API = "discovered_api_system_messages_api";
    private static final String KEY_DISCOVERED_API_USER_MESSAGES_API = "discovered_api_user_messages_api";
    private static final String KEY_DISCOVERED_API_AUTHORIZATION_ENDPOINT = "discovered_api_authorization_endpoint";

    private Context _context;

    /**
     * Constructor.
     *
     * @param context The application or activity context.
     */
    public PreferencesService(Context context) {
        _context = context;
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
        _getSharedPreferences().edit()
                .putString(KEY_INSTANCE_BASE_URI, instance.getBaseUri())
                .putString(KEY_INSTANCE_DISPLAY_NAME, instance.getDisplayName())
                .putString(KEY_INSTANCE_LOGO_URI, instance.getLogoUri())
                .apply();
    }

    /**
     * Returns a saved instance.
     *
     * @return The instance to connect to. Null if none found.
     */
    public Instance getSavedInstance() {
        String baseUri = _getSharedPreferences().getString(KEY_INSTANCE_BASE_URI, null);
        String displayName = _getSharedPreferences().getString(KEY_INSTANCE_DISPLAY_NAME, null);
        String logoUri = _getSharedPreferences().getString(KEY_INSTANCE_LOGO_URI, null);
        if (baseUri != null && displayName != null) {
            return new Instance(baseUri, displayName, logoUri);
        } else {
            return null;
        }
    }

    /**
     * Saves the current profile as the selected one.
     *
     * @param profile The profile to save.
     */
    public void saveProfile(Profile profile) {
        _getSharedPreferences().edit()
                .putString(KEY_PROFILE_POOL_ID, profile.getPoolId())
                .putString(KEY_PROFILE_DISPLAY_NAME, profile.getDisplayName())
                .putString(KEY_PROFILE_TWO_FACTOR, profile.getTwoFactor() == null ? null : profile.getTwoFactor().toString())
                .apply();
    }

    /**
     * Returns the previously saved profile.
     *
     * @return The lastly saved profile with {@link #saveProfile(Profile)}.
     */
    public Profile getSavedProfile() {
        String poolId = _getSharedPreferences().getString(KEY_PROFILE_POOL_ID, null);
        String displayName = _getSharedPreferences().getString(KEY_PROFILE_DISPLAY_NAME, null);
        String twoFactorString = _getSharedPreferences().getString(KEY_PROFILE_TWO_FACTOR, null);
        Boolean twoFactor = twoFactorString == null ? null : Boolean.valueOf(twoFactorString);
        if (poolId != null && displayName != null) {
            return new Profile(displayName, poolId, twoFactor);
        } else {
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
        _getSharedPreferences().edit()
                .putInt(KEY_DISCOVERED_API_VERSION, discoveredAPI.getApiVersion())
                .putString(KEY_DISCOVERED_API_AUTHORIZATION_ENDPOINT, discoveredAPI.getAuthorizationEndpoint())
                .putString(KEY_DISCOVERED_API_CREATE_CONFIG_API, discoveredAPI.getCreateConfigAPI())
                .putString(KEY_DISCOVERED_API_PROFILE_LIST_API, discoveredAPI.getProfileListAPI())
                .putString(KEY_DISCOVERED_API_SYSTEM_MESSAGES_API, discoveredAPI.getSystemMessagesAPI())
                .putString(KEY_DISCOVERED_API_USER_MESSAGES_API, discoveredAPI.getUserMessagesAPI())
                .apply();
    }

    /**
     * Returns a previously saved discovered API.
     *
     * @return A discovered API if saved, otherwise null.
     */
    public DiscoveredAPI getDiscoveredAPI() {
        Integer apiVersion = _getSharedPreferences().getInt(KEY_DISCOVERED_API_VERSION, -1);
        String authorizationEndpoint = _getSharedPreferences().getString(KEY_DISCOVERED_API_AUTHORIZATION_ENDPOINT, null);
        String createConfigAPI = _getSharedPreferences().getString(KEY_DISCOVERED_API_CREATE_CONFIG_API, null);
        String profileListAPI = _getSharedPreferences().getString(KEY_DISCOVERED_API_PROFILE_LIST_API, null);
        String userMessagesAPI = _getSharedPreferences().getString(KEY_DISCOVERED_API_USER_MESSAGES_API, null);
        String systemMessagesAPI = _getSharedPreferences().getString(KEY_DISCOVERED_API_SYSTEM_MESSAGES_API, null);
        if (authorizationEndpoint != null && createConfigAPI != null && profileListAPI !=null) {
            return new DiscoveredAPI(apiVersion, authorizationEndpoint, createConfigAPI,
                    profileListAPI, systemMessagesAPI, userMessagesAPI);
        } else {
            return null;
        }
    }
}
