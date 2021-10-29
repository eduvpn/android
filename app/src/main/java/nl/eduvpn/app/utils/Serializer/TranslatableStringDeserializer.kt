package nl.eduvpn.app.utils.Serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import nl.eduvpn.app.entity.TranslatableString

object TranslatableStringDeserializer : KSerializer<TranslatableString> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("TranslatableString", JsonObject.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: TranslatableString) {
        val jsonObject = JsonObject(value.translations.mapValues { (_, v) -> JsonPrimitive(v) })
        encoder.encodeSerializableValue(
            JsonObject.serializer(),
            jsonObject
        )
    }

    override fun deserialize(decoder: Decoder): TranslatableString {
        return when (val jsonElement = decoder.decodeSerializableValue(JsonElement.serializer())) {
            // Plain string was previously used to store displayName in Instance. If this is removed
            // from the deserializer, a migration is necessary.
            is JsonPrimitive -> if (jsonElement.isString) TranslatableString(jsonElement.content) else null
            is JsonObject -> TranslatableString(jsonElement.filterValues { v -> v is JsonPrimitive && v.isString }
                .mapValues { m ->
                    val translation = m.value as JsonPrimitive
                    translation.content
                })
            else -> null
        } ?: throw SerializationException("Invalid translatable string")
    }
}
