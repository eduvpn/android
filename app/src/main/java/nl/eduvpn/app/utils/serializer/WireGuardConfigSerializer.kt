package nl.eduvpn.app.utils.serializer

import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.BufferedReader
import java.io.StringReader

object WireGuardConfigSerializer : KSerializer<Config> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WireGuardConfig", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Config) {
        encoder.encodeString(value.toWgQuickString())
    }

    override fun deserialize(decoder: Decoder): Config {
        val wgQuickString = decoder.decodeString()
        return try {
            Config.parse(BufferedReader(StringReader(wgQuickString)))
        } catch (ex: BadConfigException) {
            throw SerializationException("Invalid translatable string")
        }
    }
}
