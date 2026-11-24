package dev.chieppa.ease.agent.catalog

import dev.chieppa.ease.annotations.SerializedAs
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Writes a [Stamp] as a [StampData]. */
@SerializedAs(StampData::class)
internal object StampSerializer : KSerializer<Stamp> {
  override val descriptor: SerialDescriptor = StampData.serializer().descriptor

  override fun serialize(encoder: Encoder, value: Stamp) {
    encoder.encodeSerializableValue(StampData.serializer(), StampData(value.tick))
  }

  override fun deserialize(decoder: Decoder): Stamp =
    Stamp(decoder.decodeSerializableValue(StampData.serializer()).tick)
}
