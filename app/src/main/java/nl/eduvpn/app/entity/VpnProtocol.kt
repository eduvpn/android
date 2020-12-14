package nl.eduvpn.app.entity

import com.wireguard.config.Config

sealed class VpnProtocol

data class OpenVPN(val profile: Profile) : VpnProtocol()

data class WireGuard(val config: Config) : VpnProtocol()
