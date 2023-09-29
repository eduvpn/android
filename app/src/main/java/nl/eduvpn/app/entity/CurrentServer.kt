package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

@Serializable
data class CurrentServer(
    @SerialName("custom_server")
    val customServer: CustomServer?
) {
    fun getUniqueId(): String {
        return customServer?.identifier ?: ""
    }

    fun getDisplayName(): TranslatableString {
        return customServer?.displayName ?: TranslatableString()
    }
}

@Serializable
data class CustomServer(
    val identifier: String,

    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString
)


