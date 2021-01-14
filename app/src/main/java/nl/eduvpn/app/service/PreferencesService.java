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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import net.openid.appauth.AuthState;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import nl.eduvpn.app.Constants;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Organization;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedAuthState;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.entity.ServerList;
import nl.eduvpn.app.entity.Settings;
import nl.eduvpn.app.entity.VpnProtocol;
import nl.eduvpn.app.utils.Log;

/**
 * This service is used to save temporary data
 * Created by Daniel Zolnai on 2016-10-11.
 */

@SuppressWarnings("WeakerAccess")
public class PreferencesService {
    private static final String TAG = PreferencesService.class.getName();

    private static final int STORAGE_VERSION = 4;

    private static final String KEY_PREFERENCES_NAME = "app_preferences";

    private static final String KEY_AUTH_STATE = "auth_state";
    private static final String KEY_APP_SETTINGS = "app_settings";

    static final String KEY_ORGANIZATION = "organization";
    static final String KEY_INSTANCE = "instance";

    static final String CURRENT_VPN = "current_vpn";
    @Deprecated
    static final String KEY_PROFILE = "profile";
    static final String KEY_PROFILE_LIST = "profile_list";
    static final String KEY_DISCOVERED_API = "discovered_api";

    static final String KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION = "last_known_organization_list_version";
    static final String KEY_LAST_KNOWN_SERVER_LIST_VERSION = "last_known_server_list_version";

    static final String KEY_INSTANCE_LIST_PREFIX = "instance_list_";

    @Deprecated
    static final String KEY_INSTANCE_LIST_SECURE_INTERNET = KEY_INSTANCE_LIST_PREFIX + "secure_internet";
    @Deprecated
    static final String KEY_INSTANCE_LIST_INSTITUTE_ACCESS = KEY_INSTANCE_LIST_PREFIX + "institute_access";

    static final String KEY_SERVER_LIST_DATA = "server_list_data";
    static final String KEY_SERVER_LIST_TIMESTAMP = "server_list_timestamp";


    static final String KEY_SAVED_PROFILES = "saved_profiles";
    static final String KEY_SAVED_AUTH_STATES = "saved_auth_state";
    static final String KEY_SAVED_KEY_PAIRS = "saved_key_pairs";
    static final String KEY_SAVED_ORGANIZATION = "saved_organization";
    static final String KEY_PREFERRED_COUNTRY = "preferred_country";

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
        if (version < 2) {
            SharedPreferences.Editor editor = newPreferences.edit();

            editor.putString(KEY_INSTANCE, oldPreferences.getString(KEY_INSTANCE, null));
            editor.putString(KEY_PROFILE, oldPreferences.getString(KEY_PROFILE, null));
            editor.putString(KEY_DISCOVERED_API, oldPreferences.getString(KEY_DISCOVERED_API, null));
            editor.putString(KEY_AUTH_STATE, oldPreferences.getString(KEY_AUTH_STATE, null));
            editor.putString(KEY_APP_SETTINGS, oldPreferences.getString(KEY_APP_SETTINGS, null));
            editor.putString(KEY_SAVED_PROFILES, oldPreferences.getString(KEY_SAVED_PROFILES, null));
            editor.putString(KEY_SAVED_AUTH_STATES, oldPreferences.getString(KEY_SAVED_AUTH_STATES, null));
            editor.putString(KEY_SAVED_KEY_PAIRS, oldPreferences.getString(KEY_SAVED_KEY_PAIRS, null));

            editor.putInt(KEY_STORAGE_VERSION, 3);

            editor.commit();
            oldPreferences.edit().clear().commit();
            if (Constants.DEBUG) {
                Log.d(TAG, "Migrated over to storage version v2.");
            }
        }
        if (version < 3) {
            SharedPreferences.Editor editor = newPreferences.edit();
            editor.remove(KEY_INSTANCE_LIST_SECURE_INTERNET);
            editor.remove(KEY_INSTANCE_LIST_INSTITUTE_ACCESS);
            editor.commit();
            if (Constants.DEBUG) {
                Log.d(TAG, "Migrated over to storage version v3.");
            }
        }
        if (version < 4) {
            SharedPreferences.Editor editor = newPreferences.edit();
            Profile profile = getCurrentProfile();
            if (profile != null) {
                setCurrentVPN(new VpnProtocol.OpenVPN(profile));
            }
            editor.remove(KEY_PROFILE);
            editor.putInt(KEY_STORAGE_VERSION, 4);
            editor.commit();
            if (Constants.DEBUG) {
                Log.d(TAG, "Migrated over to storage version v4.");
            }
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
     * Saves the organization the app is connecting to.
     *
     * @param organization The organization to save.
     */
    public void setCurrentOrganization(@Nullable Organization organization) {
        try {
            if (organization == null) {
                _getSharedPreferences().edit().remove(KEY_ORGANIZATION).apply();
            } else {
                _getSharedPreferences().edit()
                        .putString(KEY_ORGANIZATION, _serializerService.serializeOrganization(organization).toString())
                        .apply();
            }
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Cannot save organization!", ex);
        }
    }

    /**
     * Returns a saved organization.
     *
     * @return The organization to connect to. Null if none found.
     */
    public @Nullable
    Organization getCurrentOrganization() {
        String serializedOrganization = _getSharedPreferences().getString(KEY_ORGANIZATION, null);
        if (serializedOrganization == null) {
            return null;
        }
        try {
            return _serializerService.deserializeOrganization(new JSONObject(serializedOrganization));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize instance!", ex);
            return null;
        }
    }


    /**
     * Saves the instance the app will connect to.
     *
     * @param instance The instance to save.
     */
    public void setCurrentInstance(@Nullable Instance instance) {
        try {
            if (instance == null) {
                _getSharedPreferences().edit().remove(KEY_INSTANCE).apply();
            } else {
                _getSharedPreferences().edit()
                        .putString(KEY_INSTANCE, _serializerService.serializeInstance(instance).toString())
                        .apply();
            }
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
     * Saves the current vpn as the selected one.
     *
     * @param vpnProtocol The vpn to save.
     */
    public void setCurrentVPN(@Nullable VpnProtocol vpnProtocol) {
        try {
            if (vpnProtocol == null) {
                _getSharedPreferences().edit().remove(CURRENT_VPN).apply();
            } else {
                _getSharedPreferences().edit()
                        .putString(CURRENT_VPN, _serializerService.serializeVpnProtocol(vpnProtocol).toString())
                        .apply();
            }
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Unable to serialize current vpn!", ex);
        }
    }

    /**
     * Returns the previously saved vpn.
     *
     * @return The lastly saved vpn with {@link #setCurrentVPN(VpnProtocol)}.
     */
    @Nullable
    public VpnProtocol getCurrentVPN() {
        String serializedVPN = _getSharedPreferences().getString(CURRENT_VPN, null);
        if (serializedVPN == null) {
            return null;
        }
        try {
            return _serializerService.deserializeVpnProtocol(new JSONObject(serializedVPN));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize saved profile!", ex);
            return null;
        }
    }

    /**
     * Returns the previously saved profile.
     *
     * @return The lastly saved profile.
     */
    @Deprecated
    @Nullable
    private Profile getCurrentProfile() {
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
     * Sets the current profile list, which is all the profiles the user can connect to for the current server.
     *
     * @param currentProfileList All the profiles available on the current instance. Use null to delete all values.
     */
    public void setCurrentProfileList(@Nullable List<Profile> currentProfileList) {
        try {
            if (currentProfileList == null) {
                _getSharedPreferences().edit().remove(KEY_PROFILE_LIST).apply();
            } else {
                JSONObject serializedList = _serializerService.serializeProfileList(currentProfileList);
                _getSharedPreferences().edit().putString(KEY_PROFILE_LIST, serializedList.toString()).apply();
            }
        } catch (SerializerService.UnknownFormatException ex) {
            Log.w(TAG, "Unable to serialize profile list!", ex);
        }
    }

    /**
     * Returns the list of profiles for the current instance.
     *
     * @return The available profiles on the currently selected server. Null if none.
     */
    @Nullable
    public List<Profile> getCurrentProfileList() {
        String serializedProfileList = _getSharedPreferences().getString(KEY_PROFILE_LIST, null);
        if (serializedProfileList == null) {
            return null;
        }
        try {
            return _serializerService.deserializeProfileList(new JSONObject(serializedProfileList));
        } catch (SerializerService.UnknownFormatException | JSONException ex) {
            Log.e(TAG, "Unable to deserialize profile list!", ex);
            return null;
        }
    }

    /**
     * Saves the authorization state for further usage.
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
            Log.e(TAG, "Could not deserialize saved authorization state", ex);
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
        Settings defaultSettings = new Settings(Settings.USE_CUSTOM_TABS_DEFAULT_VALUE, Settings.FORCE_TCP_DEFAULT_VALUE, Settings.USE_WIREGUARD_DEFAULT_VALUE);
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
     * Returns a previously saved list of saved authorization states.
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

    /***
     * Stores the list of saved key pairs.
     * @param savedKeyPairs The list of saved key pairs to store.
     ***/
    public void storeSavedKeyPairList(@NonNull List<SavedKeyPair> savedKeyPairs) {
        try {
            String serializedKeyPairList = _serializerService.serializeSavedKeyPairList(savedKeyPairs).toString();
            _getSharedPreferences().edit().putString(KEY_SAVED_KEY_PAIRS, serializedKeyPairList).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Cannot store saved key pair list.", ex);
        }
    }

    /**
     * Stores an organization together with its servers.
     *
     * @param organization The organization.
     */
    public void storeSavedOrganization(@Nullable Organization organization) {
        try {
            if (organization == null) {
                _getSharedPreferences().edit().remove(KEY_SAVED_ORGANIZATION).apply();
            } else {
                String serializedSavedOrganization = _serializerService.serializeOrganization(organization).toString();
                _getSharedPreferences().edit().putString(KEY_SAVED_ORGANIZATION, serializedSavedOrganization).apply();
            }
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Cannot store saved organization.", ex);
        }
    }

    /**
     * Retrieves a previously stored saved organization.
     *
     * @return The previously stored saved organization. Null if deserialization failed or no stored one found.
     */
    @Nullable
    public Organization getSavedOrganization() {
        try {
            String savedOrganizationJson = _getSharedPreferences().getString(KEY_SAVED_ORGANIZATION, null);
            if (savedOrganizationJson == null) {
                return null;
            }
            return _serializerService.deserializeOrganization(new JSONObject(savedOrganizationJson));
        } catch (Exception ex) {
            Log.e(TAG, "Cannot deserialize saved organization.", ex);
            return null;
        }

    }

    /**
     * Sets the last known organization list version.
     *
     * @param version The last known organization list version. Use null if you want to remove previously set data.
     */
    public void setLastKnownOrganizationListVersion(@Nullable Long version) {
        if (version == null) {
            _getSharedPreferences().edit().remove(KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION).apply();
        } else {
            _getSharedPreferences().edit().putLong(KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION, version).apply();
        }
    }

    /**
     * Returns the last known organization list version.
     *
     * @return The last known organization list version. Null if no previously set value has been found.
     */
    @Nullable
    public Long getLastKnownOrganizationListVersion() {
        if (_getSharedPreferences().contains(KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION)) {
            return _getSharedPreferences().getLong(KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION, Long.MIN_VALUE);
        } else {
            return null;
        }
    }

    /**
     * Sets the last known server list version.
     *
     * @param version The last known server list version. Use null if you want to remove previously set data.
     */
    public void setLastKnownServerListVersion(@Nullable Long version) {
        if (version == null) {
            _getSharedPreferences().edit().remove(KEY_LAST_KNOWN_SERVER_LIST_VERSION).apply();
        } else {
            _getSharedPreferences().edit().putLong(KEY_LAST_KNOWN_SERVER_LIST_VERSION, version).apply();
        }
    }

    /**
     * Returns the last known server list version.
     *
     * @return The last known server list version. Null if no previously set value has been found.
     */
    @Nullable
    public Long getLastKnownServerListVersion() {
        if (_getSharedPreferences().contains(KEY_LAST_KNOWN_SERVER_LIST_VERSION)) {
            return _getSharedPreferences().getLong(KEY_LAST_KNOWN_SERVER_LIST_VERSION, Long.MIN_VALUE);
        } else {
            return null;
        }
    }

    /**
     * Returns the previously stored preferred country.
     *
     * @return The preferred country, if saved previously. Null if no saved one yet.
     */
    @Nullable
    public String getPreferredCountry() {
        return _getSharedPreferences().getString(KEY_PREFERRED_COUNTRY, null);
    }

    /**
     * Saves the preferred country selected by the user.
     *
     * @param preferredCountry The country selected by the user.
     */
    public void setPreferredCountry(@Nullable String preferredCountry) {
        if (preferredCountry == null) {
            _getSharedPreferences().edit().remove(KEY_PREFERRED_COUNTRY).apply();
        } else {
            _getSharedPreferences().edit().putString(KEY_PREFERRED_COUNTRY, preferredCountry).apply();
        }
    }

    /**
     * Returns the server list if it is recent (see constants for exact TTL).
     *
     * @return The server list if it is recent, otherwise null.
     */
    @Nullable
    public ServerList getServerList() {
        long timestamp = _getSharedPreferences().getLong(KEY_SERVER_LIST_TIMESTAMP, 0L);
        if (System.currentTimeMillis() - timestamp < Constants.SERVER_LIST_VALID_FOR_MS) {
            String serializedServerList = _getSharedPreferences().getString(KEY_SERVER_LIST_DATA, null);
            if (serializedServerList == null) {
                return null;
            }
            try {
                return _serializerService.deserializeServerList(new JSONObject(serializedServerList));
            } catch (Exception ex) {
                Log.w(TAG, "Unable to parse server list!", ex);
                return null;
            }
        } else {
            _getSharedPreferences().edit()
                    .remove(KEY_SERVER_LIST_DATA)
                    .remove(KEY_SERVER_LIST_TIMESTAMP)
                    .apply();
            return null;
        }
    }

    /**
     * Caches the server list. Only valid for a set amount, see constants for the exact TTL.
     *
     * @param serverList The server list to cache. Use null to remove previously set values.
     */
    public void setServerList(@Nullable ServerList serverList) {
        if (serverList == null) {
            _getSharedPreferences().edit().remove(KEY_SERVER_LIST_DATA)
                    .remove(KEY_SERVER_LIST_TIMESTAMP)
                    .apply();
        } else {
            try {
                String serializedServerList = _serializerService.serializeServerList(serverList).toString();
                _getSharedPreferences().edit()
                        .putString(KEY_SERVER_LIST_DATA, serializedServerList)
                        .putLong(KEY_SERVER_LIST_TIMESTAMP, System.currentTimeMillis())
                        .apply();
            } catch (Exception ex) {
                Log.w(TAG, "Unable to set server list!");
            }

        }
    }
}
