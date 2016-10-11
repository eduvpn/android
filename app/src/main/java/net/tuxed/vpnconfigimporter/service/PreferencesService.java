package net.tuxed.vpnconfigimporter.service;

import android.content.Context;
import android.content.SharedPreferences;

import net.tuxed.vpnconfigimporter.entity.Instance;

/**
 * This service is used to save temporary data
 * Created by Daniel Zolnai on 2016-10-11.
 */

public class PreferencesService {

    private static final String PREFERENCES_NAME = "preferences_service";

    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_STATE = "state";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private static final String KEY_INSTANCE_DISPLAY_NAME = "instance_display_name";
    private static final String KEY_INSTANCE_BASE_URI = "instance_base_uri";
    private static final String KEY_INSTANCE_LOGO_URI = "instance_logo_uri";

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
     * Saves the connection base URL.
     *
     * @param baseUrl The base URL to save.
     */
    public void saveConnectionBaseUrl(String baseUrl) {
        _getSharedPreferences().edit()
                .putString(KEY_BASE_URL, baseUrl)
                .apply();
    }

    /**
     * Returns the lastly saved base URL.
     *
     * @return The saved base URL. Null if none.
     */
    public String getConnectionBaseUrl() {
        return _getSharedPreferences().getString(KEY_BASE_URL, null);
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
}
