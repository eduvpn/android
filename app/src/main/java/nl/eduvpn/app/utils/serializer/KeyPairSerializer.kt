package nl.eduvpn.app.utils.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import nl.eduvpn.app.entity.KeyPair

object KeyPairSerializer : KSerializer<KeyPair> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("KeyPair", JsonElement.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: KeyPair) {
        val jsonStructure = JsonObject(
            mapOf(
                "create_keypair" to JsonObject(
                    mapOf(
                        "ok" to JsonPrimitive(value.isOK),
                        "data" to JsonObject(
                            mapOf(
                                "certificate" to JsonPrimitive(value.certificate),
                                "private_key" to JsonPrimitive(value.privateKey)
                            )
                        )
                    )
                )
            )
        )
        encoder.encodeSerializableValue(JsonObject.serializer(), jsonStructure)
    }

    override fun deserialize(decoder: Decoder): KeyPair {
        try {
            val jsonStructure =
                decoder.decodeSerializableValue(JsonObject.serializer())
            val inner = jsonStructure.getOrDefault("create_keypair", null) as JsonObject
            val ok = (inner.getOrDefault("ok", null) as JsonPrimitive).booleanOrNull as Boolean
            val data = inner.getOrDefault("data", null) as JsonObject
            val certificate = data.getOrDefault("certificate", null) as JsonPrimitive
            val privateKey = data.getOrDefault("private_key", null) as JsonPrimitive
            if (!certificate.isString || !privateKey.isString) {
                throw SerializationException("Certificate or privateKey not a string")
            }
            return KeyPair(ok, certificate.content, privateKey.content)
        } catch (ex: ClassCastException) {
            throw SerializationException("Error deserializing keypair", ex)
        }
    }
}
