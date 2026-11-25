package dev.chieppa.ease.scene.slots

import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Writes an [AssetRef] of any asset type as its GUID's text. */
internal object AssetRefSerializer : KSerializer<AssetRef<*>> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(ASSET_REF_NAME, PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: AssetRef<*>) =
    encoder.encodeString(value.guid.toString())

  override fun deserialize(decoder: Decoder): AssetRef<*> =
    AssetRef<Any>(Guid.parse(decoder.decodeString()))
}
