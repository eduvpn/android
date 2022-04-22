package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscoveredAPIs(

    @SerialName("http://eduvpn.org/api#2")
    val v2: DiscoveredAPIV2? = null,

    @SerialName("http://eduvpn.org/api#3")
    val v3: DiscoveredAPIV3? = null,
) {
    fun getPreferredAPI(): DiscoveredAPI? {
        return v3 ?: v2
    }
}
