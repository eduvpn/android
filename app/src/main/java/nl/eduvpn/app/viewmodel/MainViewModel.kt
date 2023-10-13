package nl.eduvpn.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.entity.SerializedVpnConfig
import nl.eduvpn.app.entity.VPNConfig
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.EduVPNOpenVPNService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.VPNConnectionService
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
    sealed class MainParentAction {
        data class OpenLink(val oAuthUrl: String) : MainParentAction()
        data class SelectCountry(val cookie: Int?) : MainParentAction()
        data class SelectProfiles(val profileList: List<Profile>): MainParentAction()
        data class ConnectWithConfig(val config: SerializedVpnConfig) : MainParentAction()
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
            connectWithConfig = { config ->
                _mainParentAction.postValue(MainParentAction.ConnectWithConfig(config))
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

    fun parseConfigAndStartConnection(activity: MainActivity, config: SerializedVpnConfig) {
        preferencesService.setCurrentProtocol(config.protocol)
        val parsedConfig = if (config.protocol == Protocol.OpenVPN.nativeValue) {
            eduVpnOpenVpnService.importConfig(
                config.config,
                null,
            )?.let {
                VPNConfig.OpenVPN(it)
            } ?: throw IllegalArgumentException("Unable to parse profile")
        } else if (config.protocol == Protocol.WireGuard.nativeValue) {
            VPNConfig.WireGuard(Config.parse(BufferedReader(StringReader(config.config))))
        } else {
            throw IllegalArgumentException("Unexpected protocol type: ${config.protocol}")
        }
        vpnConnectionService.connectionToConfig(viewModelScope, activity, parsedConfig)
    }

    suspend fun handleRedirection(data: Uri?) : Boolean {
        val result = backendService.handleRedirection(data)
        backendService.cancelPendingRedirect()
        return result
    }

    fun useCustomTabs() = preferencesService.getAppSettings().useCustomTabs()
    fun getCountryList(activity: MainActivity, cookie: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allInstances = organizationService.fetchServerList().serverList
                val countryList = allInstances.filter {
                    it.authorizationType == AuthorizationType.Distributed && it.countryCode != null
                }.map {
                    Pair(it, it.getCountryText() ?: "Unknown country")
                }
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(activity)
                        .setItems(countryList.map { it.second }.toTypedArray()) { _, which ->
                            val selectedInstance = countryList[which]
                            selectedInstance.first.countryCode?.let { countryCode ->
                                viewModelScope.launch {
                                    withContext(Dispatchers.IO) {
                                        backendService.selectCountry(cookie, countryCode)
                                        historyService.load()
                                    }
                                }
                            }
                        }.show()
                }
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


}