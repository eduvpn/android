package nl.eduvpn.app.entity

import com.wireguard.config.Config
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.entity.v3.SupportedProtocol
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer
import nl.eduvpn.app.utils.serializer.WireGuardConfigSerializer

sealed class ProfileV3 : Profile() {
    abstract val defaultGateway: Boolean
    abstract val expiry: Long?
    abstract val serverPreferredProtocol: SupportedProtocol

    abstract fun updateExpiry(expiry: Long?): ProfileV3
}

// The preferred protocol is stored in the type, instead of having a property 'preferredProtocol'.
// An OpenVPNProfileV3 can support WireGuard and a WireGuardProfileV3 can support OpenVPN. If
// OpenVPN is preferred then OpenVPNProfileV3 is used, if WireGuard is preferred then
// WireGuardProfileV3 is used. This way we can easily use when(..) on the protocol and create
// functions that only work with a specific protocol. The preferred protocol can change however,
// forcing us to change the type of the profile.

@Parcelize
@Serializable
@SerialName("OpenVPNProfileV3")
data class OpenVPNProfileV3(
    @SerialName("profile_id")
    override val profileId: String,

    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    override val displayName: TranslatableString,

    @SerialName("default_gateway")
    override val defaultGateway: Boolean,

    @SerialName("expiry")
    override val expiry: Long?,

    @SerialName("server_preferred_protocol")
    override val serverPreferredProtocol: SupportedProtocol,
) : ProfileV3() {
    override fun updateExpiry(expiry: Long?): ProfileV3 = copy(expiry = expiry)
}

@Parcelize
@Serializable
@SerialName("WireGuardProfileV3")
data class WireGuardProfileV3(

    @SerialName("profile_id")
    override val profileId: String,

    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    override val displayName: TranslatableString,

    @SerialName("default_gateway")
    override val defaultGateway: Boolean,

    @SerialName("config")
    @Serializable(with = WireGuardConfigSerializer::class)
    val config: @RawValue Config?, //todo: do not use @RawValue

    @SerialName("expiry")
    override val expiry: Long?,

    @SerialName("server_preferred_protocol")
    override val serverPreferredProtocol: SupportedProtocol,

    @SerialName("supports_openvpn")
    val supportsOpenVPN: Boolean,
) : ProfileV3() {
    override fun updateExpiry(expiry: Long?): ProfileV3 = copy(expiry = expiry)
}
