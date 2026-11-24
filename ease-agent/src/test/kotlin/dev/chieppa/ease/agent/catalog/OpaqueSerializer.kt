package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Writes an [Opaque] as a string, which the schema generator is not told. */
internal object OpaqueSerializer : KSerializer<Opaque> {
  override val descriptor: SerialDescriptor = String.serializer().descriptor

  override fun serialize(encoder: Encoder, value: Opaque) {
    encoder.encodeString(value.code.toString())
  }

  override fun deserialize(decoder: Decoder): Opaque = Opaque(decoder.decodeString().toInt())
}
