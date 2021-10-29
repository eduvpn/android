package nl.eduvpn.app.entity

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@SerialName("ProfileV2")
data class ProfileV2(

    // How this profile should appear in the UI
    @SerialName("display_name")
    override val displayName: String,

    // The pool ID of this VPN profile
    @SerialName("profile_id")
    override val profileId: String
) : Profile()
