package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileV2List(
    @SerialName("profile_list")
    val profileList: JsonListWrapper<ProfileV2>
)
