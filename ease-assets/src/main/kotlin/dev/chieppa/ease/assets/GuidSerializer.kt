package dev.chieppa.ease.assets

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Writes a [Guid] as its UUID text. */
internal object GuidSerializer : KSerializer<Guid> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("ease.Guid", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Guid) = encoder.encodeString(value.toString())

  override fun deserialize(decoder: Decoder): Guid = Guid.parse(decoder.decodeString())
}
