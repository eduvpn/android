enum class SupportedProtocol {
    OpenVPN,
    WireGuard;

    companion object {
        fun fromString(vpnProtocol: String): SupportedProtocol? {
            return when (vpnProtocol) {
                "openvpn" -> OpenVPN
                "wireguard" -> WireGuard
                else -> null
            }
        }
    }
}

