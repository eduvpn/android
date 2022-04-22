package nl.eduvpn.app.utils.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.openid.appauth.AuthState

object AuthStateSerializer : KSerializer<AuthState> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AuthState", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AuthState) {
        val jsonString = value.jsonSerializeString()
        encoder.encodeString(jsonString)
    }

    override fun deserialize(decoder: Decoder): AuthState {
        val jsonString = decoder.decodeString()
        return AuthState.jsonDeserialize(jsonString)
    }
}
