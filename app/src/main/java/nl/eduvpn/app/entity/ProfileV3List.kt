package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileV3List(
    @SerialName("profile_list")
    val profileList: List<ProfileV3>
)
