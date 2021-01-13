package nl.eduvpn.app.entity

sealed class VpnProtocol

data class OpenVPN(val profile: Profile) : VpnProtocol()

object WireGuard : VpnProtocol()
