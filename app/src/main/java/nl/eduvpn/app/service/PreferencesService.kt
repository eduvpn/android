/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.service


import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.VisibleForTesting
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.Constants
import nl.eduvpn.app.entity.*
import nl.eduvpn.app.entity.v3.Protocol
import nl.eduvpn.app.utils.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * This service is used to save temporary data
 * Created by Daniel Zolnai on 2016-10-11.
 */

/**
 * @param serializerService The serializer service used to serialize and deserialize objects.
 */
class PreferencesService(
    applicationContext: Context,
    serializerService: SerializerService,
) {
    companion object {
        private val TAG = PreferencesService::class.simpleName

        private const val STORAGE_VERSION = 4

        private const val KEY_PREFERENCES_NAME = "app_preferences"

        private const val KEY_AUTH_STATE = "auth_state"
        private const val KEY_APP_SETTINGS = "app_settings"

        const val KEY_ORGANIZATION = "organization"
        const val KEY_INSTANCE = "instance"
        const val KEY_PROFILE = "profile"
        const val KEY_VPN_PROTOCOL = "vpn_protocol"
        const val KEY_PROFILE_LIST = "profile_list"
        const val KEY_DISCOVERED_API = "discovered_api"

        const val KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION = "last_known_organization_list_version"
        const val KEY_LAST_KNOWN_SERVER_LIST_VERSION = "last_known_server_list_version"

        const val KEY_INSTANCE_LIST_PREFIX = "instance_list_"

        @Deprecated("")
        const val KEY_INSTANCE_LIST_SECURE_INTERNET = KEY_INSTANCE_LIST_PREFIX + "secure_internet"

        @Deprecated("")
        const val KEY_INSTANCE_LIST_INSTITUTE_ACCESS =
            KEY_INSTANCE_LIST_PREFIX + "institute_access"

        const val KEY_SERVER_LIST_DATA = "server_list_data"
        const val KEY_SERVER_LIST_TIMESTAMP = "server_list_timestamp"

        const val KEY_SAVED_AUTH_STATES = "saved_auth_state"
        const val KEY_SAVED_KEY_PAIRS = "saved_key_pairs"
        const val KEY_SAVED_ORGANIZATION = "saved_organization"
        const val KEY_PREFERRED_COUNTRY = "preferred_country"

        const val KEY_STORAGE_VERSION = "storage_version"
    }

    private val _serializerService: SerializerService = serializerService
    private val _sharedPreferences: SharedPreferences =
        applicationContext.getSharedPreferences(KEY_PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        migrateIfNeeded(_sharedPreferences, applicationContext)
    }

    @SuppressLint("ApplySharedPref")
    @VisibleForTesting
    fun migrateIfNeeded(
        newPreferences: SharedPreferences,
        applicationContext: Context,
    ) {
        val version = newPreferences.getInt(KEY_STORAGE_VERSION, 1)
        if (version < 3) {
            val editor = newPreferences.edit()
            @Suppress("DEPRECATION")
            editor.remove(KEY_INSTANCE_LIST_SECURE_INTERNET)
            @Suppress("DEPRECATION")
            editor.remove(KEY_INSTANCE_LIST_INSTITUTE_ACCESS)

            editor.commit()
            if (Constants.DEBUG) {
                Log.d(TAG, "Migrated over to storage version v3.")
            }
        }
        if (version < 4) {
            // Remove the old preference file used by com.scottyab:secure-preferences:
            // nl.eduvpn.app_preferences.xml or org.letsconnect_vpn.app_preferences.xml.

            val preferenceName = BuildConfig.APPLICATION_ID + "_preferences"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                applicationContext.deleteSharedPreferences(preferenceName)
            } else {
                val dataDir = applicationContext.filesDir.parent
                if (dataDir != null) {
                    val file =
                        File(dataDir + File.separator + "shared_prefs" + File.separator + preferenceName + ".xml")
                    try {
                        file.delete()
                    } catch (e: IOException) {

                    }
                }
            }

            val editor = newPreferences.edit()
            editor.putInt(KEY_STORAGE_VERSION, STORAGE_VERSION)
            editor.commit()
            if (Constants.DEBUG) {
                Log.d(TAG, "Migrated over to storage version v4.")
            }
        }
    }

    /**
     * Returns the shared preferences to be used throughout this service.
     *
     * @return The preferences to be used.
     */
    @VisibleForTesting
    fun getSharedPreferences(): SharedPreferences {
        return _sharedPreferences
    }

    /**
     * Clears the shared preferences, but makes sure that the storage version key remains.
     * Only use this method to clear all data.
     */
    @SuppressLint("ApplySharedPref")
    @VisibleForTesting
    fun clearPreferences() {
        _sharedPreferences.edit().clear().putInt(KEY_STORAGE_VERSION, 2).commit()
    }

    /**
     * Saves the organization the app is connecting to.
     *
     * @param organization The organization to save.
     */
    fun setCurrentOrganization(organization: Organization?) {
        try {
            if (organization == null) {
                getSharedPreferences().edit().remove(KEY_ORGANIZATION).apply()
            } else {
                getSharedPreferences().edit()
                    .putString(
                        KEY_ORGANIZATION,
                        _serializerService.serializeOrganization(organization).toString()
                    )
                    .apply()
            }
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Cannot save organization!", ex)
        }
    }

    /**
     * Returns a saved organization.
     *
     * @return The organization to connect to. Null if none found.
     */
    fun getCurrentOrganization(): Organization? {
        val serializedOrganization = getSharedPreferences().getString(KEY_ORGANIZATION, null)
            ?: return null
        return try {
            _serializerService.deserializeOrganization(JSONObject(serializedOrganization))
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to deserialize instance!", ex)
            null
        } catch (ex: JSONException) {
            Log.e(TAG, "Unable to deserialize instance!", ex)
            null
        }
    }

    /**
     * Saves the instance the app will connect to.
     *
     * @param instance The instance to save.
     */
    fun setCurrentInstance(instance: Instance?) {
        try {
            if (instance == null) {
                getSharedPreferences().edit().remove(KEY_INSTANCE).apply()
            } else {
                getSharedPreferences().edit()
                    .putString(
                        KEY_INSTANCE,
                        _serializerService.serializeInstance(instance).toString()
                    )
                    .apply()
            }
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Can not save connection instance!", ex)
        }
    }

    /**
     * Returns a saved instance.
     *
     * @return The instance to connect to. Null if none found.
     */
    fun getCurrentInstance(): Instance? {
        val serializedInstance = getSharedPreferences().getString(KEY_INSTANCE, null)
            ?: return null
        return try {
            _serializerService.deserializeInstance(serializedInstance)
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to deserialize instance!", ex)
            null
        }
    }

    /**
     * Saves the current profile as the selected one.
     *
     * @param profile  The profile to save.
     * @param protocol Protocol used by the profile, null if not known yet.
     */
    fun setCurrentProfile(profile: Profile?, protocol: Protocol?) {
        setCurrentProtocol(protocol)
        try {
            if (profile == null) {
                getSharedPreferences().edit().remove(KEY_PROFILE).apply()
            } else {
                getSharedPreferences().edit()
                    .putString(KEY_PROFILE, _serializerService.serializeProfile(profile))
                    .apply()
            }
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to serialize profile!", ex)
        }
    }

    /**
     * Returns the previously saved profile.
     *
     * @return The lastly saved profile with {@link #setCurrentProfile(Profile)}.
     */
    fun getCurrentProfile(): Profile? {
        val serializedProfile = getSharedPreferences().getString(KEY_PROFILE, null)
            ?: return null
        return try {
            _serializerService.deserializeProfile(serializedProfile)
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to deserialize saved profile!", ex)
            null
        }
    }


    /**
     * Sets the current profile list, which is all the profiles the user can connect to for the current server.
     *
     * @param currentProfileList All the profiles available on the current instance. Use null to delete all values.
     */
    fun setCurrentProfileList(currentProfileList: List<Profile>?) {
        try {
            if (currentProfileList == null) {
                getSharedPreferences().edit().remove(KEY_PROFILE_LIST).apply()
            } else {
                val serializedList = _serializerService.serializeProfileList(currentProfileList)
                getSharedPreferences().edit().putString(KEY_PROFILE_LIST, serializedList)
                    .apply()
            }
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.w(TAG, "Unable to serialize profile list!", ex)
        }
    }

    /**
     * Returns the list of profiles for the current instance.
     *
     * @return The available profiles on the currently selected server. Null if none.
     */
    fun getCurrentProfileList(): List<Profile>? {
        val serializedProfileList = getSharedPreferences().getString(KEY_PROFILE_LIST, null)
            ?: return null
        return try {
            _serializerService.deserializeProfileList(serializedProfileList)
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to deserialize profile list!", ex)
            null
        }
    }

    /**
     * Returns the saved app settings, or the default settings if none found.
     *
     * @return True if the user does not want to use Custom Tabs. Otherwise false.
     */
    fun getAppSettings(): Settings {
        val defaultSettings =
            Settings(Settings.USE_CUSTOM_TABS_DEFAULT_VALUE, Settings.FORCE_TCP_DEFAULT_VALUE)
        val serializedSettings = getSharedPreferences().getString(KEY_APP_SETTINGS, null)
        return if (serializedSettings == null) {
            // Default settings.
            storeAppSettings(defaultSettings)
            defaultSettings
        } else {
            try {
                _serializerService.deserializeAppSettings(JSONObject(serializedSettings))
            } catch (ex: Exception) {
                when (ex) {
                    is SerializerService.UnknownFormatException, is JSONException -> {
                        Log.e(TAG, "Unable to deserialize app settings!", ex)
                        storeAppSettings(defaultSettings)
                        defaultSettings
                    }
                    else -> throw ex
                }
            }
        }
    }

    /**
     * Saves the app settings.
     *
     * @param settings The settings of the app to save.
     */
    fun storeAppSettings(settings: Settings) {
        try {
            val serializedSettings = _serializerService.serializeAppSettings(settings).toString()
            getSharedPreferences().edit().putString(KEY_APP_SETTINGS, serializedSettings).apply()
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to serialize and save app settings!")
        }
    }

    /**
     * Saves a discovered API object for future usage.
     *
     * @param discoveredAPI The discovered API.
     */
    fun setCurrentDiscoveredAPI(discoveredAPI: DiscoveredAPI?) {
        try {
            val editor = getSharedPreferences().edit()
            if (discoveredAPI == null) {
                editor.remove(KEY_DISCOVERED_API)
            } else {
                editor.putString(
                    KEY_DISCOVERED_API,
                    _serializerService.serializeDiscoveredAPIs(discoveredAPI.toDiscoveredAPIs())
                )
            }
            editor.apply()
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Can not save discovered API!", ex)
        }
    }

    /**
     * Returns a previously saved discovered API.
     *
     * @return A discovered API if saved, otherwise null.
     */
    fun getCurrentDiscoveredAPI(): DiscoveredAPIV3? {
        val serializedDiscoveredAPI =
            getSharedPreferences().getString(KEY_DISCOVERED_API, null)
                ?: return null
        return try {
            _serializerService.deserializeDiscoveredAPIs(serializedDiscoveredAPI).v3
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to deserialize saved discovered API", ex)
            null
        }
    }

    fun getSavedKeyPairList(): List<SavedKeyPair>? {
        return try {
            val serializedKeyPairList = getSharedPreferences().getString(KEY_SAVED_KEY_PAIRS, null)
                ?: return null
            _serializerService.deserializeSavedKeyPairList(serializedKeyPairList)
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Cannot retrieve saved key pair list.", ex)
            null
        }
    }

    /***
     * Stores the list of saved key pairs.
     * @param savedKeyPairs The list of saved key pairs to store.
     */
    fun storeSavedKeyPairList(savedKeyPairs: List<SavedKeyPair?>) {
        try {
            val serializedKeyPairList =
                _serializerService.serializeSavedKeyPairList(savedKeyPairs).toString()
            getSharedPreferences().edit().putString(KEY_SAVED_KEY_PAIRS, serializedKeyPairList)
                .apply()
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Cannot store saved key pair list.", ex)
        }
    }

    /**
     * Stores an organization together with its servers.
     *
     * @param organization The organization.
     */
    fun storeSavedOrganization(organization: Organization?) {
        try {
            if (organization == null) {
                getSharedPreferences().edit().remove(KEY_SAVED_ORGANIZATION).apply()
            } else {
                val serializedSavedOrganization =
                    _serializerService.serializeOrganization(organization).toString()
                getSharedPreferences().edit()
                    .putString(KEY_SAVED_ORGANIZATION, serializedSavedOrganization).apply()
            }
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Cannot store saved organization.", ex)
        }
    }

    /**
     * Retrieves a previously stored saved organization.
     *
     * @return The previously stored saved organization. Null if deserialization failed or no stored one found.
     */
    fun getSavedOrganization(): Organization? {
        return try {
            val savedOrganizationJson =
                getSharedPreferences().getString(KEY_SAVED_ORGANIZATION, null)
                    ?: return null
            _serializerService.deserializeOrganization(JSONObject(savedOrganizationJson))
        } catch (ex: Exception) {
            Log.e(TAG, "Cannot deserialize saved organization.", ex)
            null
        }
    }

    /**
     * Sets the last known organization list version.
     *
     * @param version The last known organization list version. Use null if you want to remove previously set data.
     */
    fun setLastKnownOrganizationListVersion(version: Long?) {
        if (version == null) {
            getSharedPreferences().edit().remove(KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION)
                .apply()
        } else {
            getSharedPreferences().edit()
                .putLong(KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION, version).apply()
        }
    }

    /**
     * Returns the last known organization list version.
     *
     * @return The last known organization list version. Null if no previously set value has been found.
     */
    fun getLastKnownOrganizationListVersion(): Long? {
        return if (getSharedPreferences().contains(KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION)) {
            getSharedPreferences().getLong(
                KEY_LAST_KNOWN_ORGANIZATION_LIST_VERSION,
                Long.MIN_VALUE
            )
        } else {
            null
        }
    }

    /**
     * Sets the last known server list version.
     *
     * @param version The last known server list version. Use null if you want to remove previously set data.
     */
    fun setLastKnownServerListVersion(version: Long?) {
        if (version == null) {
            getSharedPreferences().edit().remove(KEY_LAST_KNOWN_SERVER_LIST_VERSION).apply()
        } else {
            getSharedPreferences().edit().putLong(KEY_LAST_KNOWN_SERVER_LIST_VERSION, version)
                .apply()
        }
    }

    /**
     * Returns the last known server list version.
     *
     * @return The last known server list version. Null if no previously set value has been found.
     */
    fun getLastKnownServerListVersion(): Long? {
        if (getSharedPreferences().contains(KEY_LAST_KNOWN_SERVER_LIST_VERSION)) {
            return getSharedPreferences().getLong(
                KEY_LAST_KNOWN_SERVER_LIST_VERSION,
                Long.MIN_VALUE
            )
        } else {
            return null
        }
    }

    /**
     * Returns the previously stored preferred country.
     *
     * @return The preferred country, if saved previously. Null if no saved one yet.
     */
    fun getPreferredCountry(): String? {
        return getSharedPreferences().getString(KEY_PREFERRED_COUNTRY, null)
    }

    /**
     * Saves the preferred country selected by the user.
     *
     * @param preferredCountry The country selected by the user.
     */
    fun setPreferredCountry(preferredCountry: String?) {
        if (preferredCountry == null) {
            getSharedPreferences().edit().remove(KEY_PREFERRED_COUNTRY).apply()
        } else {
            getSharedPreferences().edit().putString(KEY_PREFERRED_COUNTRY, preferredCountry)
                .apply()
        }
    }

    /**
     * Returns the server list if it is recent (see constants for exact TTL).
     *
     * @return The server list if it is recent, otherwise null.
     */
    fun getServerList(): ServerList? {
        val timestamp = getSharedPreferences().getLong(KEY_SERVER_LIST_TIMESTAMP, 0L)
        return if (System.currentTimeMillis() - timestamp < Constants.SERVER_LIST_VALID_FOR_MS) {
            val serializedServerList =
                getSharedPreferences().getString(KEY_SERVER_LIST_DATA, null)
                    ?: return null
            try {
                _serializerService.deserializeServerList(serializedServerList)
            } catch (ex: Exception) {
                Log.w(TAG, "Unable to parse server list!", ex)
                null
            }
        } else {
            getSharedPreferences().edit()
                .remove(KEY_SERVER_LIST_DATA)
                .remove(KEY_SERVER_LIST_TIMESTAMP)
                .apply()
            null
        }
    }

    /**
     * Caches the server list. Only valid for a set amount, see constants for the exact TTL.
     *
     * @param serverList The server list to cache. Use null to remove previously set values.
     */
    fun setServerList(serverList: ServerList?) {
        if (serverList == null) {
            getSharedPreferences().edit().remove(KEY_SERVER_LIST_DATA)
                .remove(KEY_SERVER_LIST_TIMESTAMP)
                .apply()
        } else {
            try {
                val serializedServerList = _serializerService.serializeServerList(serverList)
                getSharedPreferences().edit()
                    .putString(KEY_SERVER_LIST_DATA, serializedServerList)
                    .putLong(KEY_SERVER_LIST_TIMESTAMP, System.currentTimeMillis())
                    .apply()
            } catch (ex: Exception) {
                Log.w(TAG, "Unable to set server list!")
            }
        }
    }

    private fun setCurrentProtocol(protocol: Protocol?) {
        try {
            if (protocol == null) {
                getSharedPreferences().edit().remove(KEY_VPN_PROTOCOL).apply()
            } else {
                getSharedPreferences().edit()
                    .putString(KEY_VPN_PROTOCOL, _serializerService.serializeProtocol(protocol))
                    .apply()
            }
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to serialize protocol!", ex)
        }
    }

    fun getCurrentProtocol(): Protocol? {
        val serializedProtocol = getSharedPreferences().getString(KEY_VPN_PROTOCOL, null)
            ?: return null
        return try {
            _serializerService.deserializeProtocol(serializedProtocol)
        } catch (ex: SerializerService.UnknownFormatException) {
            Log.e(TAG, "Unable to deserialize saved protocol!", ex)
            null
        }
    }
}
