import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// V3 Profile as retrieved from the API
@Serializable
data class ProfileV3API(

    @SerialName("profile_id")
    val profileId: String,

    @SerialName("display_name")
    val displayName: String, //todo: should support multiple languages

    @SerialName("default_gateway")
    val defaultGateway: Boolean,

    @SerialName("vpn_proto_list")
    val vpnProtocolList: List<String>,

    @SerialName("vpn_proto_preferred")
    val vpnProtocolPreferred: String,
)
