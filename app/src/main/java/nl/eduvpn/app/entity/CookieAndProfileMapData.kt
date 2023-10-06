package nl.eduvpn.app.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.eduvpn.app.entity.v3.ProfileV3API
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

@Serializable
data class CookieAndProfileMapData(
    val data: ProfileWithoutIdMap,
    val cookie: Int
)

@Serializable
data class ProfileWithoutIdMap(
    internal val map: Map<String, ProfileWithoutId>,
    @SerialName("current")
    internal val currentProfileId: String?
) {
    fun getProfileList(): List<ProfileV3API> {
        return map.map {
            ProfileV3API(
                profileId = it.key,
                displayName = it.value.displayName,
                vpnProtocolList = it.value.supportedProtocols
            )
        }
    }

    val currentProfile: ProfileV3API? = getProfileList().firstOrNull { it.profileId == currentProfileId }
}

@Serializable
data class ProfileWithoutId(
    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString,

    @SerialName("supported_protocols")
    val supportedProtocols: List<Int>,
)