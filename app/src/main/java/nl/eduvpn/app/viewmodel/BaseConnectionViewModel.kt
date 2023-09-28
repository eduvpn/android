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
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.*
import nl.eduvpn.app.entity.exception.EduVPNException
import nl.eduvpn.app.entity.v3.Protocol
import nl.eduvpn.app.livedata.toSingleEvent
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
    private val backendService: BackendService,
    private val serializerService: SerializerService,
    private val historyService: HistoryService,
    private val preferencesService: PreferencesService,
    private val eduVpnOpenVpnService: EduVPNOpenVPNService,
    private val vpnConnectionService: VPNConnectionService,
) : ViewModel() {

    sealed class ParentAction {
        data class DisplayError(@StringRes val title: Int, val message: String) : ParentAction()
        data class OpenProfileSelector(val profiles: List<Profile>) : ParentAction()
        data class InitiateConnection(val instance: Instance, val authStringToOpen: String) :
            ParentAction()

        data class ConnectWithConfig(val vpnConfig: VPNConfig) : ParentAction()
    }

    val connectionState =
        MutableLiveData<ConnectionState>().also { it.value = ConnectionState.Ready }

    val warning = MutableLiveData<String>()

    val _parentAction = MutableLiveData<ParentAction?>()
    val parentAction = _parentAction.toSingleEvent()

    fun discoverApi(instance: Instance) {
        // If no discovered API, fetch it first, then initiate the connection for the login
        connectionState.value = ConnectionState.DiscoveringApi
        // Discover the API
        viewModelScope.launch(Dispatchers.IO) {
            runCatchingCoroutine {
                backendService.addServer(instance)
            }.onSuccess { result ->
                authorize(instance, result)
            }.onFailure { throwable ->
                Log.e(TAG, "Error while fetching discovered API.", throwable)
                connectionState.postValue(ConnectionState.Ready)
                _parentAction.postValue(ParentAction.DisplayError(
                    R.string.error_dialog_title,
                    context.getString(
                        R.string.error_discover_api,
                        instance.sanitizedBaseURI,
                        throwable.toString()
                    )
                ))
            }
        }
    }

    public fun getProfiles(
        instance: Instance
    ): Result<List<Profile>> {
        val apiProfiles = backendService.getProfiles(instance, false) // TODO get settings
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
        profile: Profile
    ): Result<Unit> {
        connectionState.value = ConnectionState.ProfileDownloadingKeyPair
        /**
        val (protocol, configString, expireDate) = runCatchingCoroutine {
            /**
            val keyPair = com.wireguard.crypto.KeyPair()
            val (protocol, configString, expireDate) = fetchProfileConfiguration(
                discoveredAPI,
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
            )**/
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
        _parentAction.value = ParentAction.ConnectWithConfig(vpnConfig)**/
        return Result.success(Unit)
    }

    public fun selectProfileToConnectTo(profile: Profile) : Result<Unit> {
        // TODO
        return Result.failure(Throwable())
    }
    private suspend fun selectProfile(profiles: List<Profile>): Result<Unit> {
        preferencesService.setCurrentProfileList(profiles)
        connectionState.value = ConnectionState.Ready
        return if (profiles.size > 1) {
            _parentAction.value = ParentAction.OpenProfileSelector(profiles)
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
        _parentAction.value = ParentAction.DisplayError(
            R.string.error_dialog_title,
            message
        )
        return Result.failure(thr ?: RuntimeException(message))
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


    private fun authorize(instance: Instance, authStringToOpen: String) {
        connectionState.postValue(ConnectionState.Authorizing)
        _parentAction.postValue(ParentAction.InitiateConnection(instance, authStringToOpen))
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
