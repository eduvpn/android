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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.Constants
import nl.eduvpn.app.entity.*
import nl.eduvpn.app.utils.Log
import org.eduvpn.common.Protocol
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

        private const val STORAGE_VERSION = 5

        private const val KEY_OLD_PREFERENCES_NAME = "app_preferences"
        private const val KEY_SECURE_PREFERENCES_NAME = "secure_app_preferences"

        private const val KEY_APP_SETTINGS = "app_settings"
        private const val KEY_PREFIX_SERVER_TOKEN = "server_token_"

        const val KEY_INSTANCE = "instance"
        const val KEY_VPN_PROTOCOL = "vpn_protocol"
        const val KEY_INSTANCE_LIST_PREFIX = "instance_list_"

        @Deprecated("")
        const val KEY_INSTANCE_LIST_SECURE_INTERNET = KEY_INSTANCE_LIST_PREFIX + "secure_internet"

        @Deprecated("")
        const val KEY_INSTANCE_LIST_INSTITUTE_ACCESS =
            KEY_INSTANCE_LIST_PREFIX + "institute_access"

        const val KEY_STORAGE_VERSION = "storage_version"
    }

    private val _serializerService: SerializerService = serializerService

    private val securePreferences: SharedPreferences

    init {
        val insecurePreferences: SharedPreferences =
            applicationContext.getSharedPreferences(KEY_OLD_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        securePreferences = EncryptedSharedPreferences.create(
            applicationContext,
            KEY_SECURE_PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        migrateIfNeeded(insecurePreferences, securePreferences, applicationContext)
    }

    @SuppressLint("ApplySharedPref")
    @VisibleForTesting
    fun migrateIfNeeded(
        insecurePreferences: SharedPreferences,
        securePreferences: SharedPreferences,
        applicationContext: Context,
    ) {
        val version = insecurePreferences.getInt(KEY_STORAGE_VERSION, 1)
        if (version < 3) {
            val editor = insecurePreferences.edit()
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
        }
        if (version < 5) {
            securePreferences.edit()
                .putInt(KEY_STORAGE_VERSION, 5)
                .putString(KEY_APP_SETTINGS, insecurePreferences.getString(KEY_APP_SETTINGS, null))
                .apply()
            // Remove everything else
            insecurePreferences.edit().clear().commit()
            if (Constants.DEBUG) {
                Log.d(TAG, "Migrated over to storage version v5.")
            }
        }
        val editor = insecurePreferences.edit()
        editor.putInt(KEY_STORAGE_VERSION, STORAGE_VERSION)
        editor.commit()
        if (Constants.DEBUG) {
            Log.d(TAG, "Migrated over to storage version v4.")
        }
    }

    /**
     * Returns the shared preferences to be used throughout this service.
     *
     * @return The preferences to be used.
     */
    @VisibleForTesting
    fun getSharedPreferences(): SharedPreferences {
        return securePreferences
    }

    /**
     * Clears the shared preferences, but makes sure that the storage version key remains.
     * Only use this method to clear all data.
     */
    @SuppressLint("ApplySharedPref")
    @VisibleForTesting
    fun clearPreferences() {
        securePreferences.edit()
            .clear()
            .putInt(KEY_STORAGE_VERSION, STORAGE_VERSION)
            .commit()
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
     * Returns the saved app settings, or the default settings if none found.
     *
     * @return True if the user does not want to use Custom Tabs. Otherwise false.
     */
    fun getAppSettings(): Settings {
        val defaultSettings =
            Settings(Settings.USE_CUSTOM_TABS_DEFAULT_VALUE, Settings.PREFER_TCP_DEFAULT_VALUE)
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

    fun setCurrentProtocol(protocol: Int) {
        getSharedPreferences().edit()
            .putInt(KEY_VPN_PROTOCOL, protocol)
            .apply()
    }

    fun getCurrentProtocol(): Int {
        return getSharedPreferences().getInt(KEY_VPN_PROTOCOL, Protocol.Unknown.nativeValue)
    }

    fun getToken(serverId: String): String? {
        return getSharedPreferences().getString(KEY_PREFIX_SERVER_TOKEN + serverId, null)
    }

    fun setToken(serverId: String, token: String?) {
        if (token.isNullOrEmpty()) {
            getSharedPreferences().edit().remove(KEY_PREFIX_SERVER_TOKEN + serverId).apply()
        } else {
            getSharedPreferences().edit().putString(KEY_PREFIX_SERVER_TOKEN + serverId, token).apply()
        }
    }
}
