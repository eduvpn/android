package nl.eduvpn.app.entity.v3

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.entity.ProfileV3List

@Serializable
data class InfoV3(
    @SerialName("info")
    val info: ProfileV3List
)
