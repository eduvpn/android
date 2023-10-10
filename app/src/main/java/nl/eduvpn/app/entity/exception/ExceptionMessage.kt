package nl.eduvpn.app.entity.exception

import kotlinx.serialization.Serializable
import nl.eduvpn.app.entity.TranslatableString
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

@Serializable
data class ExceptionMessage(
    @Serializable(with = TranslatableStringSerializer::class)
    val message: TranslatableString? = null
)

