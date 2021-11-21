package nl.eduvpn.app.entity

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@SerialName("ProfileV3")
data class ProfileV3(

    @SerialName("profile_id")
    override val profileId: String,

    @SerialName("display_name")
    override val displayName: String, //todo: should support multiple languages

    @SerialName("default_gateway")
    val defaultGateway: Boolean,

    @SerialName("vpn_proto_list")
    val vpnProtocolList: List<String>,

    @SerialName("vpn_proto_preferred")
    val vpnProtocolPreferred: String,

    // Expiry is not retrieved from the API via json, but we specify the SerialName so it can be
    // serialized and stored.
    @SerialName("android_expiry")
    val expiry: Long? = null,
) : Profile()
