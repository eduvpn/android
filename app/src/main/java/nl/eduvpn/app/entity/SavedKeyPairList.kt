package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavedKeyPairList(
    @SerialName("items")
    val items: List<SavedKeyPair>
)
