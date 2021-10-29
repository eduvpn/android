package nl.eduvpn.app.utils.Serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import net.openid.appauth.AuthState
import nl.eduvpn.app.utils.jsonInstance

object AuthStateSerializer : KSerializer<AuthState> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("AuthState", JsonObject.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: AuthState) {
        val jsonString = value.jsonSerializeString()
        val jsonObject = jsonInstance.decodeFromString(JsonObject.serializer(), jsonString)
        encoder.encodeSerializableValue(
            JsonObject.serializer(),
            jsonObject
        )
    }

    override fun deserialize(decoder: Decoder): AuthState {
        val jsonObject = decoder.decodeSerializableValue(JsonObject.serializer())
        return AuthState.jsonDeserialize(jsonObject.toString())
    }
}
