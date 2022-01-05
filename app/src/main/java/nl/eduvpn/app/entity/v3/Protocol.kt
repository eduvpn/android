package nl.eduvpn.app.entity.v3

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Protocol supported by this application
@Serializable
sealed class Protocol {

    @Serializable
    @SerialName("openvpn")
    object OpenVPN : Protocol()

    @Serializable
    @SerialName("wireguard")
    object WireGuard : Protocol()

    companion object {
        fun fromString(vpnProtocol: String): Protocol? {
            return when (vpnProtocol) {
                "openvpn" -> OpenVPN
                "wireguard" -> WireGuard
                else -> null
            }
        }

        fun fromContentType(vpnProtocol: String): Protocol? {
            return when (vpnProtocol) {
                "application/x-openvpn-profile" -> OpenVPN
                "application/x-wireguard-profile" -> WireGuard
                else -> null
            }
        }
    }
}
