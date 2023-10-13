package nl.eduvpn.app.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Profile (

    @SerialName("profile_id")
    val profileId: String,

    @SerialName("display_name")
    val displayName: TranslatableString,

    @SerialName("vpn_proto_list")
    val vpnProtocolList: List<Int>,
) : Parcelable
