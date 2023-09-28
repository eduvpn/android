package nl.eduvpn.app.entity.v3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.entity.TranslatableString
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

// V3 Profile as retrieved from the API
@Serializable
@Parcelize
data class ProfileV3API (

    @SerialName("profile_id")
    val profileId: String,

    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString,

    @SerialName("vpn_proto_list")
    val vpnProtocolList: List<Int>,
) : Parcelable
