package nl.eduvpn.app.entity.v3

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Info(
    @SerialName("info")
    val info: ProfileV3APIList
)
