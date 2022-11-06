package nl.eduvpn.app.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

@Parcelize
@Serializable
data class Profile(

    @SerialName("profile_id")
    val profileId: String,

    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString,

    @SerialName("expiry")
    val expiry: Long?,
) : Parcelable
