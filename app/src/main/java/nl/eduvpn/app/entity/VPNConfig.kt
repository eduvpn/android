package nl.eduvpn.app.entity

import com.wireguard.config.Config
import de.blinkt.openvpn.VpnProfile

sealed class VPNConfig {

    data class OpenVPN(val profile: VpnProfile) : VPNConfig()

    data class WireGuard(val config: Config) : VPNConfig()
}
