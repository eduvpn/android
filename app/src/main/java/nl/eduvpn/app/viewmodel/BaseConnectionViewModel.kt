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

import ProfileV3API
import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import nl.eduvpn.app.Constants
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.*
import nl.eduvpn.app.entity.exception.EduVPNException
import nl.eduvpn.app.entity.v3.Protocol
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.FormattingUtils
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.flatMap
import nl.eduvpn.app.utils.runCatchingCoroutine
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.net.URLEncoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import com.wireguard.crypto.KeyPair as WGKeyPair

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
    private val eduVpnOpenVpnService: EduVPNOpenVPNService,
    private val vpnConnectionService: VPNConnectionService,
) : ViewModel() {

    sealed class ParentAction {
        data class DisplayError(@StringRes val title: Int, val message: String) : ParentAction()
        data class OpenProfileSelector(val profiles: List<Profile>) : ParentAction()
        data class InitiateConnection(val instance: Instance, val discoveredAPI: DiscoveredAPI) :
            ParentAction()

        data class ConnectWithConfig(val vpnConfig: VPNConfig) : ParentAction()
    }

    val connectionState =
        MutableLiveData<ConnectionState>().also { it.value = ConnectionState.Ready }

    val warning = MutableLiveData<String>()

    val parentAction = MutableLiveData<ParentAction?>()

    fun discoverApi(instance: Instance, reauthorize: Boolean = false) {
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
                        serializerService.deserializeDiscoveredAPIs(result).v3
                    if (discoveredAPI == null) {
                        val errorMessage = "Server does not provide API version 3"
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
                            historyService.getSavedToken(instance)
                        if (savedToken == null || reauthorize) {
                            authorize(instance, discoveredAPI)
                        } else {
                            if (savedToken.instance.sanitizedBaseURI != instance.sanitizedBaseURI
                            ) {
                                // This is a distributed token. We add it to the list.
                                Log.i(TAG, "Distributed token found for different instance.")
                                preferencesService.setCurrentInstance(instance)
                                preferencesService.setCurrentDiscoveredAPI(discoveredAPI)
                                preferencesService.setCurrentAuthState(savedToken.authState)
                                historyService.cacheAuthorizationState(
                                    instance,
                                    savedToken.authState,
                                    savedToken.authenticationDate
                                )
                            }
                            preferencesService.setCurrentInstance(instance)
                            preferencesService.setCurrentDiscoveredAPI(discoveredAPI)
                            preferencesService.setCurrentAuthState(savedToken.authState)
                            getSupportedProfilesV3(
                                instance,
                                discoveredAPI,
                                savedToken.authState
                            ).flatMap { supportedProfiles ->
                                selectProfile(supportedProfiles)
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

    private suspend fun getSupportedProfilesV3(
        instance: Instance,
        discoveredAPI: DiscoveredAPIV3,
        authState: AuthState
    ): Result<List<Profile>> {
        val apiProfiles = fetchProfilesV3(
            instance,
            discoveredAPI,
            authState
        ).getOrElse { return Result.failure(it) }
        val supportedProfiles =
            apiProfiles.mapNotNull { profile ->
                val supportedProtocols = profile.vpnProtocolList
                    .mapNotNull { protocol -> Protocol.fromString(protocol) }
                if (supportedProtocols.isEmpty()) {
                    null
                } else {
                    Profile(
                        profile.profileId,
                        profile.displayName,
                        null,
                    )
                }
            }
        if (supportedProfiles.isEmpty() && apiProfiles.isNotEmpty()) {
            return Result.failure(
                EduVPNException(
                    R.string.error_no_profiles_from_server,
                    R.string.error_no_supported_profiles_from_server
                )
            )
        }
        return Result.success(supportedProfiles)
    }

    private suspend fun connectToProfileV3(
        instance: Instance, discoveredAPI: DiscoveredAPIV3,
        profile: Profile, authState: AuthState
    ): Result<Unit> {
        connectionState.value = ConnectionState.ProfileDownloadingKeyPair
        val (protocol, configString, expireDate) = runCatchingCoroutine {
            val keyPair = com.wireguard.crypto.KeyPair()
            val (protocol, configString, expireDate) = fetchProfileConfiguration(
                discoveredAPI,
                authState,
                profile,
                preferencesService.getAppSettings().forceTcp(),
                keyPair
            )
            when (protocol) {
                is Protocol.OpenVPN -> Triple(protocol, configString, expireDate)
                is Protocol.WireGuard -> {
                    val configStringWithPrivateKey = configString
                        .replace(
                            "[Interface]",
                            "[Interface]\n" +
                                    "PrivateKey = ${keyPair.privateKey.toBase64()}"
                        )
                    Triple(protocol, configStringWithPrivateKey, expireDate)
                }
            }
        }.getOrElse { throwable ->
            connectionState.value = ConnectionState.Ready
            return Result.failure(
                if (throwable is APIService.UserNotAuthorizedException) {
                    throwable
                } else {
                    EduVPNException(
                        R.string.unexpected_error,
                        R.string.error_downloading_vpn_config,
                        throwable
                    )
                }
            )
        }

        val updatedProfile = profile.copy(expiry = expireDate?.time)
        val vpnConfig = when (protocol) {
            is Protocol.OpenVPN -> {
                val configName = FormattingUtils.formatProfileName(
                    context,
                    instance,
                    updatedProfile
                )
                val vpnProfile = eduVpnOpenVpnService.importConfig(
                    configString,
                    configName,
                    null,
                )
                vpnProfile?.let { p -> VPNConfig.OpenVPN(p) }
            }
            is Protocol.WireGuard -> {
                val config = withContext(Dispatchers.IO) {
                    try {
                        Config.parse(BufferedReader(StringReader(configString)))
                    } catch (ex: BadConfigException) {
                        null
                    }
                }
                config?.let { c -> VPNConfig.WireGuard(c) }
            }
        }
        if (vpnConfig == null) {
            connectionState.value = ConnectionState.Ready
            return Result.failure(
                EduVPNException(
                    R.string.unexpected_error,
                    R.string.error_importing_profile
                )
            )
        }
        preferencesService.setCurrentProfile(updatedProfile, protocol)
        parentAction.value = ParentAction.ConnectWithConfig(vpnConfig)
        return Result.success(Unit)
    }

    private suspend fun selectProfile(profiles: List<Profile>): Result<Unit> {
        preferencesService.setCurrentProfileList(profiles)
        connectionState.value = ConnectionState.Ready
        return if (profiles.size > 1) {
            parentAction.value = ParentAction.OpenProfileSelector(profiles)
            Result.success(Unit)
        } else if (profiles.size == 1) {
            selectProfileToConnectTo(profiles[0])
        } else {
            Result.failure(
                EduVPNException(
                    R.string.error_no_profiles_from_server,
                    R.string.error_no_profiles_from_server_message
                )
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
    ): Result<List<ProfileV3API>> {
        connectionState.value = ConnectionState.FetchingProfiles
        return runCatchingCoroutine {
            apiService.getString(discoveredAPI.infoEndpoint, authState)
        }.onFailure { throwable ->
            Log.e(TAG, "Error fetching profile list.", throwable)
            // It is highly probable that the auth state is not valid anymore.
            // todo: do not reauthorize on server error, i.e. response code 500
            authorize(instance, discoveredAPI)
        }.flatMap { result ->
            try {
                Result.success(serializerService.deserializeInfo(result).info.profileList)
            } catch (ex: SerializerService.UnknownFormatException) {
                showError(ex, R.string.error_parsing_profiles)
            }
        }
    }

    private fun getExpiryFromHeaders(headers: Map<String, List<String>>): Date? {
        return headers["Expires"]
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
    }

    private suspend fun fetchProfileConfiguration(
        discoveredAPI: DiscoveredAPIV3,
        authState: AuthState,
        profile: Profile,
        tcpOnly: Boolean,
        keyPair: WGKeyPair,
    ): Triple<Protocol, String, Date?> {
        val tcpOnlyString = if (tcpOnly) {
            "on"
        } else {
            "off"
        }
        val base64PublicKey = URLEncoder.encode(keyPair.publicKey.toBase64(), Charsets.UTF_8.name())
        val urlEncodedProfileId = URLEncoder.encode(profile.profileId, Charsets.UTF_8.name())
        val (configString, headers) = apiService.postResource(
            discoveredAPI.connectEndpoint,
            "profile_id=${urlEncodedProfileId}&public_key=${base64PublicKey}&tcp_only=${tcpOnlyString}",
            authState
        )
        val protocolHeader = headers["Content-Type"]
            ?.let { hl: List<String> -> hl.firstOrNull() }
            ?: throw IOException("Could not determine protocol, missing Content-Type header")
        val protocol = Protocol.fromContentType(protocolHeader)
            ?: throw IOException("Unsupported protocol: $protocolHeader")
        return Triple(protocol, configString, getExpiryFromHeaders(headers))
    }

    private fun authorize(instance: Instance, discoveredAPI: DiscoveredAPI) {
        connectionState.value = ConnectionState.Authorizing
        parentAction.value = ParentAction.InitiateConnection(instance, discoveredAPI)
        parentAction.value =
            null // Immediately reset it, so it is not triggered twice, when coming back to the activity.
    }

    fun initiateConnection(activity: Activity) {
        viewModelScope.launch {
            connectionService.initiateConnection(
                activity,
                preferencesService.getCurrentInstance()!!,
                preferencesService.getCurrentDiscoveredAPI()!!
            )
        }
    }

    fun initiateConnection(activity: Activity, instance: Instance, discoveredAPI: DiscoveredAPI) {
        viewModelScope.launch {
            connectionService.initiateConnection(activity, instance, discoveredAPI)
        }
    }

    suspend fun selectProfileToConnectTo(profile: Profile): Result<Unit> {
        // We surely have a discovered API and access token, since we just loaded the list with them
        val instance = preferencesService.getCurrentInstance()
        val authState = historyService.getCachedAuthState(instance!!)?.first
        val discoveredAPI = preferencesService.getCurrentDiscoveredAPI()
        if (authState == null || discoveredAPI == null) {
            Log.e(
                TAG,
                "Unable to connect. Auth state OK: ${authState != null}, discovered API OK: ${discoveredAPI != null}"
            )
            connectionState.value = ConnectionState.Ready
            return Result.failure(
                EduVPNException(
                    R.string.unexpected_error,
                    R.string.cant_connect_application_state_missing
                )
            )
        }
        preferencesService.setCurrentProfile(profile, null)
        preferencesService.setCurrentAuthState(authState)
        return connectToProfileV3(instance, discoveredAPI, profile, authState)
    }

    fun disconnectWithCall(vpnService: VPNService) {
        vpnConnectionService.disconnect(context, vpnService)
    }

    fun deleteAllDataForInstance(instance: Instance) {
        historyService.removeAllDataForInstance(instance)
    }

    fun getProfileInstance(): Instance {
        return preferencesService.getCurrentInstance()!!
    }

    fun connectionToConfig(activity: Activity, vpnConfig: VPNConfig): VPNService {
        connectionState.value = ConnectionState.Ready
        return vpnConnectionService.connectionToConfig(viewModelScope, activity, vpnConfig)
    }

    companion object {
        private val TAG = BaseConnectionViewModel::class.java.name
    }

}
