package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eduvpn.common.Protocol

@Serializable
data class SerializedVpnConfig(
    val config: String,
    val protocol: Int,
    @SerialName("default_gateway")
    val defaultGateway: Boolean,
    @SerialName("should_failover")
    val shouldFailover: Boolean = false,
    val proxy: ProxySettings? = null
)

@Serializable
data class ProxySettings(
    @SerialName("source_port")
    val sourcePort: Int,
    val listen: String,
    val peer: String
)