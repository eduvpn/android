package nl.eduvpn.app.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.AddedServer
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.entity.SerializedVpnConfig
import nl.eduvpn.app.entity.VPNConfig
import nl.eduvpn.app.entity.exception.CommonException
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.EduVPNOpenVPNService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.VPNConnectionService
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.countryCodeToCountryNameAndFlag
import nl.eduvpn.app.utils.countryName
import nl.eduvpn.app.utils.toSingleEvent
import org.eduvpn.common.Protocol
import java.io.BufferedReader
import java.io.StringReader
import java.lang.reflect.InvocationTargetException
import javax.inject.Inject

class MainViewModel @Inject constructor(
    context: Context,
    private val historyService: HistoryService,
    private val backendService: BackendService,
    private val preferencesService: PreferencesService,
    private val eduVpnOpenVpnService: EduVPNOpenVPNService,
    private val vpnConnectionService: VPNConnectionService
) : BaseConnectionViewModel(
    context,
    backendService,
    historyService,
    preferencesService,
    vpnConnectionService
) {

    companion object {
        private val TAG = MainViewModel::class.simpleName
    }

    sealed class MainParentAction {
        data class OpenLink(val oAuthUrl: String) : MainParentAction()
        data class SelectCountry(val cookie: Int?) : MainParentAction()
        data class SelectProfiles(val profileList: List<Profile>): MainParentAction()
        data class ConnectWithConfig(val config: SerializedVpnConfig, val preferTcp: Boolean) : MainParentAction()
        data class ShowCountriesDialog(val serverWithCountries: List<Pair<AddedServer, String>>, val cookie: Int?): MainParentAction()
        data class ShowError(val throwable: Throwable) : MainParentAction()
        data object OnProxyGuardReady: MainParentAction()
    }

    private val _mainParentAction = MutableLiveData<MainParentAction>()
    val mainParentAction = _mainParentAction.toSingleEvent()

    val proxyGuardEnabled: Boolean get() = preferencesService.getCurrentProtocol() == Protocol.WireGuardWithTCP.nativeValue
    private val _failoverResult = MutableLiveData(false)
    val failoverResult = _failoverResult.toSingleEvent()


    init {
        backendService.register(
            startOAuth = { oAuthUrl ->
                _mainParentAction.postValue(MainParentAction.OpenLink(oAuthUrl))
            },
            selectCountry = { cookie ->
                _mainParentAction.postValue(MainParentAction.SelectCountry(cookie))
            },
            selectProfiles = { profileList ->
                _mainParentAction.postValue(MainParentAction.SelectProfiles(profileList))
            },
            connectWithConfig = { config, preferTcp ->
                _mainParentAction.postValue(MainParentAction.ConnectWithConfig(config, preferTcp))
            },
            showError = { throwable ->
                _mainParentAction.postValue(MainParentAction.ShowError(throwable))
            },
            protectSocket = { fd ->
                vpnConnectionService.protectSocket(viewModelScope, fd)
            },
            onProxyGuardReady = {
                _mainParentAction.postValue(MainParentAction.OnProxyGuardReady)
            }
        )
        try {
            historyService.load()
        } catch (ex: Exception) {
            Log.w(TAG, "Could not load history from the common backend on initialization!", ex)
            _mainParentAction.postValue(MainParentAction.ShowError(ex))
        }
    }

    override fun onResume() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyService.load()
                backendService.cancelPendingRedirect()
            } catch (ex: Exception) {
                Log.w(TAG, "Could not load history from the common backend on resume!", ex)
                _mainParentAction.postValue(MainParentAction.ShowError(ex))
            }
        }
        super.onResume()
    }

    override fun onCleared() {
        super.onCleared()
        backendService.deregister()
    }

    fun hasServers() = historyService.addedServers?.hasServers() == true

    fun parseConfigAndStartConnection(
        activity: MainActivity,
        config: SerializedVpnConfig,
        preferTcp: Boolean
    ) {
        var protocol = config.protocol
        if (preferTcp && protocol == Protocol.OpenVPN.nativeValue) {
            protocol = Protocol.OpenVPNWithTCP.nativeValue
        } else if (preferTcp && protocol == Protocol.WireGuard.nativeValue) {
            protocol = Protocol.WireGuardWithTCP.nativeValue
        }
        preferencesService.setCurrentProtocol(protocol)
        if (protocol == Protocol.WireGuardWithTCP.nativeValue && config.proxy != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    backendService.startProxyguard(config.proxy)
                } catch (ex: CommonException) {
                    // These are just warnings, so we log them, but don't display to the user
                    Log.w( TAG, "Unable to start Proxyguard detection", ex)
                }
            }
        }

        val parsedConfig = if (protocol == Protocol.OpenVPN.nativeValue || protocol == Protocol.OpenVPNWithTCP.nativeValue) {
            eduVpnOpenVpnService.importConfig(
                config.config,
                null,
            )?.let {
                VPNConfig.OpenVPN(it)
            } ?: throw IllegalArgumentException("Unable to parse profile")
        } else if (protocol == Protocol.WireGuard.nativeValue || protocol == Protocol.WireGuardWithTCP.nativeValue) {
            try {
                VPNConfig.WireGuard(Config.parse(BufferedReader(StringReader(config.config))))
            } catch (ex: BadConfigException) {
                // Notify the user that the config is not valid
                Log.e(TAG, "Unable to parse WireGuard config", ex)
                _mainParentAction.postValue(MainParentAction.ShowError(ex))
                return
            }
        } else {
            throw IllegalArgumentException("Unexpected protocol type: $protocol")
        }
        val service = vpnConnectionService.connectionToConfig(viewModelScope, activity, parsedConfig, preferTcp)
        if (protocol == Protocol.WireGuard.nativeValue && !preferTcp && config.shouldFailover) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Waits a bit so that the network interface has been surely set up
                    delay(1_000L)
                    backendService.startFailOver(service) {
                        _failoverResult.postValue(true)
                    }
                } catch (ex: CommonException) {
                    // These are just warnings, so we log them, but don't display to the user
                    Log.w( TAG, "Unable to start failover detection", ex)
                }
            }
        }
    }

    suspend fun handleRedirection(data: Uri?) : Boolean {
        val result = backendService.handleRedirection(data)
        backendService.cancelPendingRedirect()
        return result
    }

    fun getCountryList(cookie: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val secureInternetServer = historyService.addedServers?.secureInternetServer!!
                val locations = secureInternetServer.locations
                val countryList = locations.map {
                    Pair(secureInternetServer, it)
                }.sortedBy {
                    it.second.countryName()
                }
                _mainParentAction.postValue(MainParentAction.ShowCountriesDialog(serverWithCountries = countryList, cookie = cookie))
            } catch (ex: Exception) {
                _parentAction.postValue(
                    ParentAction.DisplayError(
                        R.string.error_countries_are_not_available,
                        ex.message ?: ex.toString()
                    )
                )
            }
        }
    }

    fun onCountrySelected(cookie: Int?, organizationId: String, countryCode: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backendService.selectCountry(cookie, organizationId, countryCode)
                withContext(Dispatchers.Main) {
                    try {
                        historyService.load()
                    } catch (ex: Exception) {
                        _mainParentAction.postValue(MainParentAction.ShowError(ex))
                    }
                }
            } catch (ex: Exception) {
                _mainParentAction.postValue(MainParentAction.ShowError(ex))
            }
        }
    }

    fun connectWithPendingConfig(activity: Activity) {
        if (vpnConnectionService.connectWithPendingConfig(viewModelScope, activity)) {
            Log.i(TAG, "Connection with pending config successful.")
        } else {
            Log.w(TAG, "Pending config not found!")
        }
    }


}