package nl.eduvpn.app.entity

import com.wireguard.config.Config

sealed class CurrentVPN

data class OpenVPN(val profile: Profile) : CurrentVPN()

data class WireGuard(val config: Config) : CurrentVPN()
