package nl.eduvpn.app.entity.exception

import kotlinx.serialization.Serializable
import nl.eduvpn.app.entity.TranslatableString

@Serializable
data class ExceptionMessage(
    val message: TranslatableString? = null,
    val misc: Boolean? = false
)

