package nl.eduvpn.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
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
import nl.eduvpn.app.utils.getCountryText
import nl.eduvpn.app.utils.toSingleEvent
import org.eduvpn.common.Protocol
import java.io.BufferedReader
import java.io.StringReader
import javax.inject.Inject

class MainViewModel @Inject constructor(
    context: Context,
    private val historyService: HistoryService,
    private val backendService: BackendService,
    private val organizationService: OrganizationService,
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
        data class ConnectWithConfig(val config: SerializedVpnConfig, val forceTCP: Boolean) : MainParentAction()
        data class ShowCountriesDialog(val instancesWithNames: List<Pair<Instance, String>>, val cookie: Int?): MainParentAction()
        data class ShowError(val throwable: Throwable) : MainParentAction()
    }

    private val _mainParentAction = MutableLiveData<MainParentAction>()
    val mainParentAction = _mainParentAction.toSingleEvent()

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
            connectWithConfig = { config, forceTcp ->
                _mainParentAction.postValue(MainParentAction.ConnectWithConfig(config, forceTcp))
            },
            showError = { throwable ->
                _mainParentAction.postValue(MainParentAction.ShowError(throwable))
            }
        )
        historyService.load()
    }

    override fun onResume() {
        historyService.load()
        backendService.cancelPendingRedirect()
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
        forceTCP: Boolean
    ) {
        preferencesService.setCurrentProtocol(config.protocol)
        val parsedConfig = if (config.protocol == Protocol.OpenVPN.nativeValue) {
            eduVpnOpenVpnService.importConfig(
                config.config,
                null,
            )?.let {
                VPNConfig.OpenVPN(it)
            } ?: throw IllegalArgumentException("Unable to parse profile")
        } else if (config.protocol == Protocol.WireGuard.nativeValue) {
            try {
                VPNConfig.WireGuard(Config.parse(BufferedReader(StringReader(config.config))))
            } catch (ex: BadConfigException) {
                // Notify the user that the config is not valid
                _mainParentAction.postValue(MainParentAction.ShowError(ex))
                return
            }
        } else {
            throw IllegalArgumentException("Unexpected protocol type: ${config.protocol}")
        }
        val service = vpnConnectionService.connectionToConfig(viewModelScope, activity, parsedConfig)
        if (config.protocol == Protocol.WireGuard.nativeValue && !forceTCP) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Waits a bit so that the network interface has been surely set up
                    delay(1_000L)
                    backendService.startFailOver(service) {
                        // Failover needed, request a new profile with TCP enforced.
                        preferencesService.getCurrentInstance()?.let { currentInstance ->
                            viewModelScope.launch {
                                // Disconnect first, otherwise we don't have any internet :)
                                service.disconnect()
                                // Wait a bit for the disconnection to finish
                                delay(500L)
                                // Fetch a new profile, now with TCP forced
                                backendService.getConfig(currentInstance, forceTCP = true)
                            }
                        }
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

    fun useCustomTabs() = preferencesService.getAppSettings().useCustomTabs()
    fun getCountryList(cookie: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allInstances = organizationService.fetchServerList().serverList
                val countryList = allInstances.filter {
                    it.authorizationType == AuthorizationType.Distributed && it.countryCode != null
                }.map {
                    Pair(it, it.getCountryText() ?: "Unknown country")
                }
                _mainParentAction.postValue(MainParentAction.ShowCountriesDialog(instancesWithNames = countryList, cookie = cookie))
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

    fun onCountrySelected(cookie: Int?, countryCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            backendService.selectCountry(cookie, countryCode)
            historyService.load()
        }
    }


}