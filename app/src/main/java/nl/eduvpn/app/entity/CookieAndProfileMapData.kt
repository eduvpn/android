package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

@Serializable
data class CookieAndProfileMapData(
    val data: ProfileWithoutIdMap,
    val cookie: Int
)

@Serializable
data class ProfileWithoutIdMap(
    internal val map: Map<String, ProfileWithoutId>? = null,
    @SerialName("current")
    internal val currentProfileId: String?
) {
    fun getProfileList(): List<Profile> {
        return map?.map {
            Profile(
                profileId = it.key,
                displayName = it.value.displayName,
                vpnProtocolList = it.value.supportedProtocols
            )
        } ?: emptyList()
    }

    val currentProfile: Profile? = getProfileList().firstOrNull { it.profileId == currentProfileId }
}

@Serializable
data class ProfileWithoutId(
    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString,

    @SerialName("supported_protocols")
    val supportedProtocols: List<Int>,
)