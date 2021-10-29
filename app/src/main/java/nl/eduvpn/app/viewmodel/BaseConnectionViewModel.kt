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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.blinkt.openvpn.VpnProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.Constants
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.*
import nl.eduvpn.app.entity.v3.VPNConfig
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.*
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * This viewmodel takes care of the entire flow, from connecting to the servers to fetching profiles.
 */
@Suppress("ConstantConditionIf")
abstract class BaseConnectionViewModel(
        private val context: Context,
        private val apiService: APIService,
        private val serializerService: SerializerService,
        private val historyService: HistoryService,
        private val preferencesService: PreferencesService,
        private val connectionService: ConnectionService,
        private val vpnService: VPNService
) : ViewModel() {

    sealed class ParentAction {
        data class DisplayError(@StringRes val title: Int, val message: String) : ParentAction()
        data class OpenProfileSelector(val profiles: List<Profile>) : ParentAction()
        data class InitiateConnection(val instance: Instance, val discoveredAPI: DiscoveredAPI) : ParentAction()
        data class ConnectWithProfile(val vpnProfile: VpnProfile) : ParentAction()
    }

    val connectionState = MutableLiveData<ConnectionState>().also { it.value = ConnectionState.Ready }

    val warning = MutableLiveData<String>()

    val parentAction = MutableLiveData<ParentAction>()

    fun discoverApi(
        instance: Instance,
        parentInstance: Instance? = null,
        reauthorize: Boolean = false
    ) {
        // If no discovered API, fetch it first, then initiate the connection for the login
        connectionState.value = ConnectionState.DiscoveringApi
        // Discover the API
        viewModelScope.launch(Dispatchers.Main) {
            runCatchingCoroutine {
                apiService.getString(
                    instance.sanitizedBaseURI + Constants.API_DISCOVERY_POSTFIX,
                    null
                )
            }.onSuccess { result ->
                try {
                    val discoveredAPI =
                        serializerService.deserializeDiscoveredAPIs(result).getPreferredAPI()
                    if (discoveredAPI == null) {
                        val errorMessage = "Server does not provide API version 2 or 3"
                        Log.e(TAG, errorMessage)
                        connectionState.value = ConnectionState.Ready
                        parentAction.value = ParentAction.DisplayError(
                            R.string.error_dialog_title,
                            context.getString(
                                R.string.error_discover_api,
                                instance.sanitizedBaseURI,
                                errorMessage
                            )
                        )
                    } else {
                        val savedToken =
                            historyService.getSavedToken(parentInstance ?: instance)
                        if (savedToken == null || reauthorize) {
                            authorize(parentInstance ?: instance, discoveredAPI)
                        } else {
                            if (savedToken.instance.sanitizedBaseURI != (parentInstance
                                    ?: instance).sanitizedBaseURI
                            ) {
                                // This is a distributed token. We add it to the list.
                                Log.i(TAG, "Distributed token found for different instance.")
                                preferencesService.currentInstance = instance
                                preferencesService.currentDiscoveredAPI = discoveredAPI
                                preferencesService.currentAuthState = savedToken.authState
                                historyService.cacheAuthorizationState(
                                    instance,
                                    savedToken.authState
                                )
                            }
                            preferencesService.currentInstance = instance
                            preferencesService.currentDiscoveredAPI = discoveredAPI
                            preferencesService.currentAuthState = savedToken.authState
                            when (discoveredAPI) {
                                is DiscoveredAPIV2 -> {
                                    fetchProfiles(instance, discoveredAPI, savedToken.authState)
                                }
                                is DiscoveredAPIV3 -> {
                                    connectV3(instance, discoveredAPI, savedToken.authState)
                                }
                            }
                        }
                    }
                } catch (ex: SerializerService.UnknownFormatException) {
                    Log.e(TAG, "Error parsing discovered API!", ex)
                    connectionState.value = ConnectionState.Ready
                    parentAction.value = ParentAction.DisplayError(
                        R.string.error_dialog_title,
                        context.getString(
                            R.string.error_discover_api,
                            instance.sanitizedBaseURI,
                            ex.toString()
                        )
                    )
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Error while fetching discovered API.", throwable)
                connectionState.value = ConnectionState.Ready
                parentAction.value = ParentAction.DisplayError(
                    R.string.error_dialog_title,
                    context.getString(
                        R.string.error_discover_api,
                        instance.sanitizedBaseURI,
                        throwable.toString()
                    )
                )
            }
        }
    }

    private suspend fun connectV3(
        instance: Instance,
        discoveredAPI: DiscoveredAPIV3,
        authState: AuthState
    ) {
        val profiles = fetchProfilesV3(instance, discoveredAPI, authState)
            .getOrElse { return }
            .filter { p -> p.vpnProtocol == "openvpn" }
        selectProfile(profiles)
    }

    private suspend fun connectToProfileV3(
        instance: Instance, discoveredAPI: DiscoveredAPIV3,
        profile: ProfileV3, authState: AuthState
    ) {
        val vpnConfig = fetchProfileConfigurationOpenVPN(
            discoveredAPI,
            authState,
            profile,
            preferencesService.appSettings.forceTcp()
        ).getOrElse { return }
        val uProfile = profile.copy(expiry = vpnConfig.expireDate?.time)
        preferencesService.currentProfile = uProfile
        val configName = FormattingUtils.formatProfileName(
            context,
            instance,
            uProfile
        )
        val vpnProfile = vpnService.importConfig(
            vpnConfig.config,
            configName,
            null,
        )
        if (vpnProfile != null) {
            // Cache the profile
            val savedProfile = SavedProfile(
                instance,
                uProfile,
                vpnProfile.uuidString
            )
            historyService.cacheSavedProfile(savedProfile)
            // Connect with the profile
            parentAction.value =
                ParentAction.ConnectWithProfile(vpnProfile)
        } else {
            connectionState.value = ConnectionState.Ready
            parentAction.value = ParentAction.DisplayError(
                R.string.error_dialog_title,
                context.getString(R.string.error_importing_profile)
            )
        }
    }

    private fun selectProfile(profiles: List<Profile>) {
        preferencesService.currentProfileList = profiles
        connectionState.value = ConnectionState.Ready
        if (profiles.size > 1) {
            parentAction.value = ParentAction.OpenProfileSelector(profiles)
        } else if (profiles.size == 1) {
            selectProfileToConnectTo(profiles[0])
        } else {
            parentAction.value = ParentAction.DisplayError(
                R.string.error_no_profiles_from_server,
                context.getString(R.string.error_no_profiles_from_server_message)
            )
        }
    }

    open fun onResume() {
        if (connectionState.value == ConnectionState.Authorizing) {
            connectionState.value = ConnectionState.Ready
        }
    }

    private fun <T> showError(thr: Throwable?, resourceId: Int): Result<T> {
        val message = context.getString(resourceId, thr)
        Log.e(TAG, message, thr)
        connectionState.value = ConnectionState.Ready
        parentAction.value = ParentAction.DisplayError(
            R.string.error_dialog_title,
            message
        )
        return Result.failure(thr ?: RuntimeException(message))
    }

    /**
     * Starts downloading the list of profiles for a single VPN provider.
     *
     * @param instance      The VPN provider instance.
     * @param discoveredAPI The discovered API containing the URLs.
     * @param authState     The access and refresh token for the API.
     */
    private fun fetchProfiles(
        instance: Instance,
        discoveredAPI: DiscoveredAPIV2,
        authState: AuthState
    ) {
        connectionState.value = ConnectionState.FetchingProfiles
        viewModelScope.launch(Dispatchers.Main) {
            runCatchingCoroutine {
                apiService.getString(discoveredAPI.profileListEndpoint, authState)
            }.onSuccess { result ->
                try {
                    val profiles: List<Profile> = serializerService.deserializeProfileV2List(result)
                    selectProfile(profiles)
                } catch (ex: SerializerService.UnknownFormatException) {
                    showError<Unit>(ex, R.string.error_parsing_profiles)
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Error fetching profile list.", throwable)
                // It is highly probable that the auth state is not valid anymore.
                authorize(instance, discoveredAPI)
            }
        }
    }

    /**
     * Downloads the list of profiles for a single VPN provider.
     *
     * @param instance      The VPN provider instance.
     * @param discoveredAPI The discovered API containing the URLs.
     * @param authState     The access and refresh token for the API.
     */
    private suspend fun fetchProfilesV3(
        instance: Instance,
        discoveredAPI: DiscoveredAPIV3,
        authState: AuthState
    ): Result<List<ProfileV3>> {
        connectionState.value = ConnectionState.FetchingProfiles
        return runCatchingCoroutine {
            apiService.getString(discoveredAPI.infoEndpoint, authState)
        }.onFailure { throwable ->
            Log.e(TAG, "Error fetching profile list.", throwable)
            // It is highly probable that the auth state is not valid anymore.
            authorize(instance, discoveredAPI)
        }.flatMap { result ->
            try {
                Result.success(serializerService.deserializeInfo(result).info.profileList)
            } catch (ex: SerializerService.UnknownFormatException) {
                showError(ex, R.string.error_parsing_profiles)
            }
        }
    }

    //todo: store and get from storage
    private suspend fun fetchProfileConfigurationOpenVPN(
        discoveredAPI: DiscoveredAPIV3,
        authState: AuthState,
        profile: ProfileV3,
        tcpOnly: Boolean,
    ): Result<VPNConfig> {
        connectionState.value = ConnectionState.ProfileDownloadingKeyPair
        return runCatchingCoroutine {
            val tcpOnlyString = if (tcpOnly) {
                "on"
            } else {
                "off"
            }
            val (config, headers) = apiService.postResource(
                discoveredAPI.connectEndpoint,
                "profile_id=${profile.profileId}&tcp_only=${tcpOnlyString}",
                authState
            )
            val expired = headers.get("Expires")
                ?.let { hl: List<String> -> hl.firstOrNull() }
                ?.let { expiredValue ->
                    try {
                        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).parse(
                            expiredValue
                        )
                    } catch (ex: ParseException) {
                        Log.e(TAG, "Unable to parse expired header", ex)
                        null
                    }
                }
            VPNConfig(config, expired)
        }.onFailure { throwable ->
            showError<Unit>(throwable, R.string.error_creating_keypair)
        }
    }

    private fun authorize(instance: Instance, discoveredAPI: DiscoveredAPI) {
        connectionState.value = ConnectionState.Authorizing
        parentAction.value = ParentAction.InitiateConnection(instance, discoveredAPI)
        parentAction.value =
            null // Immediately reset it, so it is not triggered twice, when coming back to the activity.
    }

    fun initiateConnection(activity: Activity, instance: Instance, discoveredAPI: DiscoveredAPI) {
        viewModelScope.launch {
            connectionService.initiateConnection(activity, instance, discoveredAPI)
        }
    }

    fun selectProfileToConnectTo(profile: Profile) {
        // We surely have a discovered API and access token, since we just loaded the list with them
        val instance = preferencesService.currentInstance
        val authState = historyService.getCachedAuthState(instance!!)
        val discoveredAPI = preferencesService.currentDiscoveredAPI
        if (authState == null || discoveredAPI == null) {
            Log.e(
                TAG,
                "Unable to connect. Auth state OK: ${authState != null}, discovered API OK: ${discoveredAPI != null}"
            )
            connectionState.value = ConnectionState.Ready
            ErrorDialog.show(
                context,
                R.string.error_dialog_title,
                R.string.cant_connect_application_state_missing
            )
            return
        }
        preferencesService.currentProfile = profile
        preferencesService.currentAuthState = authState
        // Always download a new profile.
        // Just to be sure,
        when (profile) {
            is ProfileV2 -> when (discoveredAPI) {
                is DiscoveredAPIV2 -> downloadKeyPairIfNeeded(
                    instance,
                    discoveredAPI,
                    profile,
                    authState
                )
                is DiscoveredAPIV3 -> throw IllegalStateException("Profile V2 with API V3")
            }
            is ProfileV3 -> when (discoveredAPI) {
                is DiscoveredAPIV2 -> throw IllegalStateException("Profile V3 with API V2")
                is DiscoveredAPIV3 ->
                    viewModelScope.launch(Dispatchers.Main) {
                        connectToProfileV3(
                            instance,
                            discoveredAPI,
                            profile,
                            authState
                        )
                    }
            }
        }
    }


    /**
     * Downloads the key pair if no cached one found. After that it downloads the profile and connects to it.
     *
     * @param instance      The VPN provider.
     * @param discoveredAPI The discovered API.
     * @param profile       The profile to download.
     */
    private fun downloadKeyPairIfNeeded(
        instance: Instance, discoveredAPI: DiscoveredAPIV2,
        profile: ProfileV2, authState: AuthState
    ) {
        // First we create a keypair, if there is no saved one yet.
        val savedKeyPair = historyService.getSavedKeyPairForInstance(instance)
        connectionState.value =
            if (savedKeyPair != null) ConnectionState.ProfileCheckingCertificate else ConnectionState.ProfileDownloadingKeyPair
        if (savedKeyPair != null) {
            checkCertificateValidity(instance, discoveredAPI, savedKeyPair, profile, authState)
            return
        }

        var requestData = "display_name=eduVPN"
        try {
            requestData =
                "display_name=" + URLEncoder.encode(BuildConfig.CERTIFICATE_DISPLAY_NAME, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            // unable to encode the display name, use default
        }

        val createKeyPairEndpoint = discoveredAPI.createKeyPairEndpoint
        viewModelScope.launch(Dispatchers.Main) {
            runCatchingCoroutine {
                apiService.postResource(createKeyPairEndpoint, requestData, authState)
            }.onSuccess { (keyPairJson, _) ->
                try {
                    val keyPair = serializerService.deserializeKeyPair(keyPairJson)
                    Log.i(TAG, "Created key pair, is it successful: " + keyPair.isOK)
                    // Save it for later
                    val newKeyPair = SavedKeyPair(instance, keyPair)
                    historyService.storeSavedKeyPair(newKeyPair)
                    downloadProfileWithKeyPair(
                        instance,
                        discoveredAPI,
                        newKeyPair,
                        profile,
                        authState
                    )
                } catch (ex: Exception) {
                    Log.e(TAG, "Unable to parse keypair data", ex)
                    connectionState.value = ConnectionState.Ready
                    parentAction.value = ParentAction.DisplayError(
                        R.string.error_dialog_title,
                        context.getString(R.string.error_parsing_keypair, ex.message)
                    )
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Error creating keypair.", throwable)
                connectionState.value = ConnectionState.Ready
                parentAction.value = ParentAction.DisplayError(
                    R.string.error_dialog_title,
                    context.getString(R.string.error_creating_keypair, throwable.toString())
                )
            }
        }
    }

    /**
     * Now that we have the key pair, we can download the profile.
     *
     * @param instance      The API instance definition.
     * @param discoveredAPI The discovered API URLs.
     * @param savedKeyPair  The private key and certificate used to generate the profile.
     * @param profile       The profile to create.
     * @param authState     Authorization state which helps us connect tot the API.
     */
    private fun downloadProfileWithKeyPair(
        instance: Instance, discoveredAPI: DiscoveredAPIV2,
        savedKeyPair: SavedKeyPair, profile: ProfileV2,
        authState: AuthState
    ) {
        val requestData = "?profile_id=" + profile.profileId
        viewModelScope.launch(Dispatchers.Main) {
            runCatchingCoroutine {
                apiService.getString(discoveredAPI.profileConfigEndpoint + requestData, authState)
            }.onSuccess { vpnConfig ->
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
                    parentAction.value = ParentAction.DisplayError(
                        R.string.error_dialog_title,
                        context.getString(R.string.error_importing_profile)
                    )
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Error fetching profile.", throwable)
                connectionState.value = ConnectionState.Ready
                parentAction.value = ParentAction.DisplayError(
                    R.string.error_dialog_title,
                    context.getString(R.string.error_fetching_profile, throwable.toString())
                )
            }
        }
    }

    private fun checkCertificateValidity(
        instance: Instance,
        discoveredAPI: DiscoveredAPIV2,
        savedKeyPair: SavedKeyPair,
        profile: ProfileV2,
        authState: AuthState
    ) {
        val commonName = savedKeyPair.keyPair.certificateCommonName
        if (commonName == null) {
            // Unable to check, better download it again.
            historyService.removeSavedKeyPairs(instance)
            // Try downloading it again.
            downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState)
            Log.w(TAG, "Could not check if certificate is valid. Downloading again.")
        }
        viewModelScope.launch(Dispatchers.Main) {
            runCatchingCoroutine {
                apiService.getJSON(discoveredAPI.getCheckCertificateEndpoint(commonName), authState)
            }.onSuccess { result ->
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
                            historyService.removeSavedKeyPairs(instance)
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
            }.onFailure { throwable ->
                connectionState.value = ConnectionState.Ready
                if (throwable is APIService.UserNotAuthorizedException || throwable.toString().contains("invalid_grant")) {
                    Log.w(TAG, "Access rejected with error.", throwable)
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, FormattingUtils.formatAccessWarning(context, instance))
                } else {
                    parentAction.value = ParentAction.DisplayError(R.string.error_dialog_title, context.getString(R.string.error_checking_certificate))
                    Log.e(TAG, "Error checking certificate.", throwable)
                }

            }
        }
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
        private val TAG = BaseConnectionViewModel::class.java.name
    }

}
