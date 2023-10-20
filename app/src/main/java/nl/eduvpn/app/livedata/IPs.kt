package nl.eduvpn.app.livedata

data class IPs(val clientIpv4: String?, val clientIpv6: String?, val tunnelData: TunnelData?)
data class TunnelData(val tunnelIp: String?, val mtu: Int?)