package nl.eduvpn.app.entity

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

@Parcelize
@Serializable
@SerialName("ProfileV3")
data class ProfileV3(

    @SerialName("profile_id")
    override val profileId: String,

    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    override val displayName: TranslatableString,

    @SerialName("expiry")
    val expiry: Long?,
) : Profile()
