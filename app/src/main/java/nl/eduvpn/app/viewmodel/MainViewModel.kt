package nl.eduvpn.app.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.entity.SerializedVpnConfig
import nl.eduvpn.app.entity.VPNConfig
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.EduVPNOpenVPNService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.VPNConnectionService
import org.eduvpn.common.Protocol
import java.io.BufferedReader
import java.io.StringReader
import javax.inject.Inject

class MainViewModel @Inject constructor(
    context: Context,
    backendService: BackendService,
    private val historyService: HistoryService,
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

    fun useCustomTabs() = preferencesService.getAppSettings().useCustomTabs()
    fun getCountryList(): List<String> {
        historyService.load()
        historyService.addedServers
        return emptyList()
    }
}