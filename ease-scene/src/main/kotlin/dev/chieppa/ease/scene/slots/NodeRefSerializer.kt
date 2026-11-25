package dev.chieppa.ease.scene.slots

import dev.chieppa.ease.scene.node.NodeKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Writes a [NodeRef] as its key's text. */
internal object NodeRefSerializer : KSerializer<NodeRef> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(NODE_REF_NAME, PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: NodeRef) = encoder.encodeString(value.key.text)

  override fun deserialize(decoder: Decoder): NodeRef =
    NodeRef(NodeKey.parse(decoder.decodeString()))
}
