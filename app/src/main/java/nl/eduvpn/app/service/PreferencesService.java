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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import net.openid.appauth.AuthState;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import nl.eduvpn.app.Constants;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.InstanceList;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedAuthState;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.entity.Settings;
import nl.eduvpn.app.utils.Log;

/**
 * This service is used to save temporary data
 * Created by Daniel Zolnai on 2016-10-11.
 */

@SuppressWarnings("WeakerAccess")
public class PreferencesService {
    private static final String TAG = PreferencesService.class.getName();

    private static final int STORAGE_VERSION = 2;

    private static final String KEY_PREFERENCES_NAME = "app_preferences";

    private static final String KEY_AUTH_STATE = "auth_state";
    private static final String KEY_APP_SETTINGS = "app_settings";

    static final String KEY_INSTANCE = "instance";
    static final String KEY_PROFILE = "profile";
    static final String KEY_DISCOVERED_API = "discovered_api";

    static final String KEY_INSTANCE_LIST_PREFIX = "instance_list_";
    static final String KEY_INSTANCE_LIST_SECURE_INTERNET = KEY_INSTANCE_LIST_PREFIX + "secure_internet";
    static final String KEY_INSTANCE_LIST_INSTITUTE_ACCESS = KEY_INSTANCE_LIST_PREFIX + "institute_access";

    static final String KEY_SAVED_PROFILES = "saved_profiles";
    static final String KEY_SAVED_AUTH_STATES = "saved_auth_state";
    static final String KEY_SAVED_KEY_PAIRS = "saved_key_pairs";
    static final String KEY_STORAGE_VERSION = "storage_version";

    private SerializerService _serializerService;
    private SharedPreferences _sharedPreferences;

    /**
     * Constructor.
     *
     * @param serializerService The serializer service used to serialize and deserialize objects.
     * @param securePreferences The deprecated secured shared preferences, used to migrate old data to the new version.
     */
    public PreferencesService(Context applicationContext, SerializerService serializerService, SharedPreferences securePreferences) {
        _sharedPreferences = applicationContext.getSharedPreferences(KEY_PREFERENCES_NAME, Context.MODE_PRIVATE);
        _serializerService = serializerService;
        _migrateIfNeeded(_sharedPreferences, securePreferences);
    }

    @SuppressLint("ApplySharedPref")
    @VisibleForTesting
    void _migrateIfNeeded(SharedPreferences newPreferences, SharedPreferences oldPreferences) {
        int version = newPreferences.getInt(KEY_STORAGE_VERSION, 1);
        if (version < STORAGE_VERSION) {
            SharedPreferences.Editor editor = newPreferences.edit();

            editor.putString(KEY_INSTANCE, oldPreferences.getString(KEY_INSTANCE, null));
            editor.putString(KEY_PROFILE, oldPreferences.getString(KEY_PROFILE, null));
            editor.putString(KEY_DISCOVERED_API, oldPreferences.getString(KEY_DISCOVERED_API, null));
            editor.putString(KEY_AUTH_STATE, oldPreferences.getString(KEY_AUTH_STATE, null));
            editor.putString(KEY_APP_SETTINGS, oldPreferences.getString(KEY_APP_SETTINGS, null));
            editor.putString(KEY_SAVED_PROFILES, oldPreferences.getString(KEY_SAVED_PROFILES, null));
            editor.putString(KEY_SAVED_AUTH_STATES, oldPreferences.getString(KEY_SAVED_AUTH_STATES, null));
            editor.putString(KEY_SAVED_KEY_PAIRS, oldPreferences.getString(KEY_SAVED_KEY_PAIRS, null));

            editor.putInt(KEY_STORAGE_VERSION, STORAGE_VERSION);

            editor.commit();
            oldPreferences.edit().clear().commit();
        }
        if (Constants.DEBUG) {
            Log.d(TAG, "Migrated over to storage version v2.");
        }
    }

    /**
     * Returns the shared preferences to be used throughout this service.
     *
     * @return The preferences to be used.
     */
    @VisibleForTesting
    SharedPreferences _getSharedPreferences() {
        return _sharedPreferences;
    }

    /**
     * Clears the shared preferences, but makes sure that the storage version key remains.
     * Only use this method to clear all data. Only to be used for unit testing
     */
    @SuppressLint("ApplySharedPref")
    @VisibleForTesting
    void _clearPreferences() {
        _sharedPreferences.edit().clear().putInt(KEY_STORAGE_VERSION, 2).commit();
    }

    /**
     * Saves the instance the app will connect to.
     *
     * @param instance The instance to save.
     */
    public void setCurrentInstance(@NonNull Instance instance) {
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
    public void setCurrentAuthState(@Nullable AuthState authState) {
        SharedPreferences.Editor editor = _getSharedPreferences().edit();
        if (authState == null) {
            editor.remove(KEY_AUTH_STATE);
        } else {
            editor.putString(KEY_AUTH_STATE, authState.jsonSerializeString());
        }
        editor.apply();
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
    public void setCurrentDiscoveredAPI(@Nullable DiscoveredAPI discoveredAPI) {
        try {
            SharedPreferences.Editor editor = _getSharedPreferences().edit();
            if (discoveredAPI == null) {
                editor.remove(KEY_DISCOVERED_API);
            } else {
                editor.putString(KEY_DISCOVERED_API, _serializerService.serializeDiscoveredAPI(discoveredAPI).toString());
            }
            editor.apply();
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
     * Stores the instance list for a specific connection type
     */
    public void storeInstanceList(AuthorizationType authorizationType, InstanceList instanceListToSave) {
        String key;
        if (authorizationType == AuthorizationType.Distributed) {
            key = KEY_INSTANCE_LIST_INSTITUTE_ACCESS;
        } else if (authorizationType == AuthorizationType.Local) {
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
    public InstanceList getInstanceList(AuthorizationType authorizationType) {
        String key;
        if (authorizationType == AuthorizationType.Distributed) {
            key = KEY_INSTANCE_LIST_INSTITUTE_ACCESS;
        } else if (authorizationType == AuthorizationType.Local) {
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
