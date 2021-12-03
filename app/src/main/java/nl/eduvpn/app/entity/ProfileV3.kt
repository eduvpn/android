package nl.eduvpn.app.entity

import com.wireguard.config.Config
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer
import nl.eduvpn.app.utils.serializer.WireGuardConfigSerializer

sealed class ProfileV3 : Profile() {
    abstract val defaultGateway: Boolean
    abstract val expiry: Long?
    abstract fun updateExpiry(expiry: Long?): ProfileV3
}

@Parcelize
@Serializable
@SerialName("OpenVpnProfileV3")
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
) : ProfileV3() {
    override fun updateExpiry(expiry: Long?): ProfileV3 = copy(expiry = expiry)
}
