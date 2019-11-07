/*
 * This file is part of eduVPN.
 *
 * eduVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * eduVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package nl.eduvpn.app.viewmodel

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import de.blinkt.openvpn.VpnProfile
import net.openid.appauth.AuthState
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.Constants
import nl.eduvpn.app.R
import nl.eduvpn.app.base.BaseViewModel
import nl.eduvpn.app.entity.DiscoveredAPI
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.entity.SavedKeyPair
import nl.eduvpn.app.entity.SavedProfile
import nl.eduvpn.app.service.APIService
import nl.eduvpn.app.service.ConfigurationService
import nl.eduvpn.app.service.ConnectionService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.SerializerService
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.FormattingUtils
import nl.eduvpn.app.utils.Log
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Observable
import java.util.Observer

/**
 * This viewmodel takes care of the entire flow, from connecting to the servers to fetching profiles.
 */
@Suppress("ConstantConditionIf")
class ConnectionViewModel(
        private val context: Context,
        private val apiService: APIService,
        private val serializerService: SerializerService,
        private val configurationService: ConfigurationService,
        private val historyService: HistoryService,
        private val preferencesService: PreferencesService,
        private val connectionService: ConnectionService,
        private val vpnService: VPNService
) : BaseViewModel(), Observer {

    enum class ConnectionState(@StringRes val displayString: Int?) {
        Ready(null),
        DiscoveringApi(R.string.api_discovery_message),
        FetchingProfiles(R.string.loading_available_profiles),
        Authenticating(R.string.loading_browser_for_authentication),
        ProfileCheckingCertificate(R.string.vpn_profile_checking_certificate),
        ProfileDownloadingKeyPair(R.string.vpn_profile_creating_keypair)
    }

    sealed class ParentAction {
        data class DisplayError(@StringRes val title: Int, val message: String) : ParentAction()
        data class OpenProfileSelector(val profiles: List<Profile>) : ParentAction()
        data class InitiateConnection(val instance: Instance, val discoveredAPI: DiscoveredAPI) : ParentAction()
        object CheckIfAutoConnectRequired : ParentAction()
        data class ConnectWithProfile(val vpnProfile: VpnProfile) : ParentAction()
    }

    val connectionState = MutableLiveData<ConnectionState>().also { it.value = ConnectionState.Ready }
    val parentAction = MutableLiveData<ParentAction>()

    val instances = MutableLiveData<List<Instance>>()

    init {
        if (BuildConfig.API_DISCOVERY_ENABLED) {
            configurationService.addObserver(this)
        }
        historyService.addObserver(this)
        refreshInstances()
    }

    override fun onCleared() {
        super.onCleared()
        if (BuildConfig.API_DISCOVERY_ENABLED) {
            configurationService.deleteObserver(this)
        }
        historyService.deleteObserver(this)
    }


    fun discoverApi(instance: Instance) {
        // If no discovered API, fetch it first, then initiate the connection for the login
        connectionState.value = ConnectionState.DiscoveringApi
        // Discover the API
        apiService.getJSON(instance.sanitizedBaseURI + Constants.API_DISCOVERY_POSTFIX, null, object : APIService.Callback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                try {
                    val discoveredAPI = serializerService.deserializeDiscoveredAPI(result)
                    val savedToken = historyService.getSavedToken(instance)
                    if (savedToken == null) {
                        authenticate(instance, discoveredAPI)
                    } else {
                        fetchProfiles(savedToken.instance, discoveredAPI, savedToken.authState)
                    }
                } catch (ex: SerializerService.UnknownFormatException) {
                    Log.e(TAG, "Error parsing discovered API!", ex)
                    connectionState.value = ConnectionState.Ready
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, ex.toString())
                }

            }

            override fun onError(errorMessage: String) {
                Log.e(TAG, "Error while fetching discovered API: $errorMessage")
                connectionState.value = ConnectionState.Ready
                parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, errorMessage)
            }
        })
    }

    fun onResume() {
        if (connectionState.value == ConnectionState.Authenticating) {
            connectionState.value = ConnectionState.Ready
        }
    }

    /**
     * Starts downloading the list of profiles for a single VPN provider.
     *
     * @param instance      The VPN provider instance.
     * @param discoveredAPI The discovered API containing the URLs.
     * @param authState     The access and refresh token for the API.
     */
    private fun fetchProfiles(instance: Instance, discoveredAPI: DiscoveredAPI, authState: AuthState) {
        connectionState.value = ConnectionState.FetchingProfiles
        apiService.getJSON(discoveredAPI.profileListEndpoint, authState, object : APIService.Callback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                try {
                    val profiles = serializerService.deserializeProfileList(result)
                    preferencesService.currentInstance = instance
                    preferencesService.currentDiscoveredAPI = discoveredAPI
                    preferencesService.currentAuthState = authState
                    connectionState.value = ConnectionState.Ready
                    parentAction.value = ParentAction.OpenProfileSelector(profiles)
                } catch (ex: SerializerService.UnknownFormatException) {
                    Log.e(TAG, "Error parsing profile list.", ex)
                    connectionState.value = ConnectionState.Ready
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, context.getString(R.string.error_parsing_profiles))
                }
            }

            override fun onError(errorMessage: String) {
                Log.e(TAG, "Error fetching profile list: $errorMessage")
                // It is highly probable that the auth state is not valid anymore.
                authenticate(instance, discoveredAPI)
            }
        })
    }

    fun tryAutoConnectIfReturningFromAuth(): Boolean {
        val currentInstance = preferencesService.currentInstance
        val discoveredAPI = preferencesService.currentDiscoveredAPI
        val authState = historyService.getCachedAuthState(currentInstance)
        return if (currentInstance != null && discoveredAPI != null && authState != null) {
            Log.d(TAG, "Continuing with profile fetch after successful auth.")
            fetchProfiles(currentInstance, discoveredAPI, authState)
            true
        } else {
            // Auth state might not be processed yet.
            Log.i(TAG, "Not all data available after an auth redirect. Instance OK: ${currentInstance != null}, " +
                    "discovery OK: ${discoveredAPI != null}, auth OK: ${authState != null}")
            false
        }
    }

    private fun authenticate(instance: Instance, discoveredAPI: DiscoveredAPI) {
        connectionState.value = ConnectionState.Authenticating
        parentAction.value = ParentAction.InitiateConnection(instance, discoveredAPI)
        parentAction.value = null // Immediately reset it, so it is not triggered twice, when coming back to the activity.
    }

    fun initiateConnection(activity: Activity, instance: Instance, discoveredAPI: DiscoveredAPI) {
        connectionService.initiateConnection(activity, instance, discoveredAPI)
    }

    fun selectProfileToConnectTo(profile: Profile) {
        // We surely have a discovered API and access token, since we just loaded the list with them
        val instance = preferencesService.currentInstance
        val authState = historyService.getCachedAuthState(instance!!)
        val discoveredAPI = preferencesService.currentDiscoveredAPI
        if (authState == null || discoveredAPI == null) {
            Log.e(TAG, "Unable to connect. Auth state OK: ${authState != null}, discovered API OK: ${discoveredAPI != null}")
            connectionState.value = ConnectionState.Ready
            ErrorDialog.show(context, R.string.error_dialog_title, R.string.cant_connect_application_state_missing)
            return
        }
        preferencesService.currentInstance = instance
        preferencesService.storeCurrentProfile(profile)
        preferencesService.currentAuthState = authState
        // Always download a new profile.
        // Just to be sure,
        downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState)
    }


    /**
     * Downloads the key pair if no cached one found. After that it downloads the profile and connects to it.
     *
     * @param instance      The VPN provider.
     * @param discoveredAPI The discovered API.
     * @param profile       The profile to download.
     */
    private fun downloadKeyPairIfNeeded(instance: Instance, discoveredAPI: DiscoveredAPI,
                                        profile: Profile, authState: AuthState) {
        // First we create a keypair, if there is no saved one yet.
        val savedKeyPair = historyService.getSavedKeyPairForInstance(instance)
        connectionState.value = if (savedKeyPair != null) ConnectionState.ProfileCheckingCertificate else ConnectionState.ProfileDownloadingKeyPair
        if (savedKeyPair != null) {
            checkCertificateValidity(instance, discoveredAPI, savedKeyPair, profile, authState)
            return
        }

        var requestData = "display_name=eduVPN"
        try {
            requestData = "display_name=" + URLEncoder.encode(BuildConfig.CERTIFICATE_DISPLAY_NAME, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            // unable to encode the display name, use default
        }

        val createKeyPairEndpoint = discoveredAPI.createKeyPairEndpoint
        apiService.postResource(createKeyPairEndpoint, requestData, authState, object : APIService.Callback<String> {

            override fun onSuccess(keyPairJson: String) {
                try {
                    val keyPair = serializerService.deserializeKeyPair(JSONObject(keyPairJson))
                    Log.i(TAG, "Created key pair, is it successful: " + keyPair.isOK)
                    // Save it for later
                    val newKeyPair = SavedKeyPair(instance, keyPair)
                    historyService.storeSavedKeyPair(newKeyPair)
                    downloadProfileWithKeyPair(instance, discoveredAPI, newKeyPair, profile, authState)
                } catch (ex: Exception) {
                    Log.e(TAG, "Unable to parse keypair data", ex)
                    connectionState.value = ConnectionState.Ready
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, context.getString(R.string.error_parsing_keypair, ex.message))
                }

            }

            override fun onError(errorMessage: String) {
                Log.e(TAG, "Error creating keypair: $errorMessage")
                connectionState.value = ConnectionState.Ready
                parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, context.getString(R.string.error_creating_keypair, errorMessage))
            }
        })
    }

    /**
     * Now that we have the key pair, we can download the profile.
     *
     * @param instance      The API instance definition.
     * @param discoveredAPI The discovered API URLs.
     * @param savedKeyPair  The private key and certificate used to generate the profile.
     * @param profile       The profile to create.
     * @param dialog        Loading dialog which should be dismissed when finished.
     */
    private fun downloadProfileWithKeyPair(instance: Instance, discoveredAPI: DiscoveredAPI,
                                           savedKeyPair: SavedKeyPair, profile: Profile,
                                           authState: AuthState) {
        val requestData = "?profile_id=" + profile.profileId
        apiService.getString(discoveredAPI.profileConfigEndpoint + requestData, authState, object : APIService.Callback<String> {
            override fun onSuccess(vpnConfig: String) {
                // The downloaded profile misses the <cert> and <key> fields. We will insert that via the saved key pair.
                val configName = FormattingUtils.formatProfileName(context, instance, profile)
                val vpnProfile = vpnService.importConfig(vpnConfig, configName, savedKeyPair)
                if (vpnProfile != null) {
                    // Cache the profile
                    val savedProfile = SavedProfile(instance, profile, vpnProfile.uuidString)
                    historyService.cacheSavedProfile(savedProfile)
                    // Connect with the profile
                    parentAction.value = ParentAction.ConnectWithProfile(vpnProfile)
                } else {
                    connectionState.value = ConnectionState.Ready
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, context.getString(R.string.error_importing_profile))
                }
            }

            override fun onError(errorMessage: String) {
                Log.e(TAG, "Error fetching profile: $errorMessage")
                connectionState.value = ConnectionState.Ready
                parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, context.getString(R.string.error_fetching_profile, errorMessage))
            }
        })
    }

    private fun checkCertificateValidity(instance: Instance, discoveredAPI: DiscoveredAPI, savedKeyPair: SavedKeyPair, profile: Profile, authState: AuthState) {
        val commonName = savedKeyPair.keyPair.certificateCommonName
        if (commonName == null) {
            // Unable to check, better download it again.
            historyService.removeSavedKeyPairs(instance)
            // Try downloading it again.
            downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState)
            Log.w(TAG, "Could not check if certificate is valid. Downloading again.")
        }
        apiService.getJSON(discoveredAPI.getCheckCertificateEndpoint(commonName), authState, object : APIService.Callback<JSONObject> {

            override fun onSuccess(result: JSONObject) {
                try {
                    val isValid = result.getJSONObject("check_certificate").getJSONObject("data").getBoolean("is_valid")
                    if (isValid) {
                        Log.i(TAG, "Certificate appears to be valid.")
                        downloadProfileWithKeyPair(instance, discoveredAPI, savedKeyPair, profile, authState)
                    } else {
                        val reason = result.getJSONObject("check_certificate").getJSONObject("data").getString("reason")
                        if ("user_disabled" == reason || "certificate_disabled" == reason) {
                            var errorStringId = R.string.error_certificate_disabled
                            if ("user_disabled" == reason) {
                                errorStringId = R.string.error_user_disabled
                            }
                            connectionState.value = ConnectionState.Ready
                            parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title_invalid_certificate, context.getString(errorStringId))
                        } else {
                            // Remove stored keypair.
                            historyService.removeSavedKeyPairs(instance)
                            Log.i(TAG, "Certificate is invalid. Fetching new one. Reason: $reason")
                            // Try downloading it again.
                            downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState)
                        }

                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Unexpected certificate call response!", ex)
                    connectionState.value = ConnectionState.Ready
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, context.getString(R.string.error_parsing_certificate))
                }

            }

            override fun onError(errorMessage: String?) {
                connectionState.value = ConnectionState.Ready
                if (errorMessage != null && (APIService.USER_NOT_AUTHORIZED_ERROR == errorMessage || errorMessage.contains("invalid_grant"))) {
                    Log.w(TAG, "Access rejected with error $errorMessage")
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, FormattingUtils.formatAccessWarning(context, instance))
                } else {
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, context.getString(R.string.error_checking_certificate))
                    Log.e(TAG, "Error checking certificate: $errorMessage")
                }

            }
        })
    }



    override fun update(o: Observable?, arg: Any?) {
        if (o is HistoryService) {
            refreshInstances()
            // Tokens are processed at a later moment
            parentAction.value = ParentAction.CheckIfAutoConnectRequired
        } else if (o is ConfigurationService) {
            refreshInstances()
        }
    }

    private fun refreshInstances() {
        val newInstances = historyService.savedAuthStateList.map { it.instance }.toMutableList()
        instances.value = newInstances
    }

    fun deleteAllDataForInstance(instance: Instance) {
        historyService.removeAllDataForInstance(instance)
    }

    fun getProfileInstance(): Instance {
        return preferencesService.currentInstance
    }

    fun openVpnConnectionToProfile(activity: Activity, vpnProfile: VpnProfile) {
        connectionState.value = ConnectionState.Ready
        vpnService.connect(activity, vpnProfile)
    }


    companion object {
        private const val TAG = "VPN-Connection"
    }

}