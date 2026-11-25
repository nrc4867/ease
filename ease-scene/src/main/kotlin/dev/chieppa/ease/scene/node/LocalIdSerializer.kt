package dev.chieppa.ease.scene.node

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Writes a [LocalId] as its 16 hex digits. */
internal object LocalIdSerializer : KSerializer<LocalId> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("ease.LocalId", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: LocalId) = encoder.encodeString(value.text)

  override fun deserialize(decoder: Decoder): LocalId = LocalId.parse(decoder.decodeString())
}
