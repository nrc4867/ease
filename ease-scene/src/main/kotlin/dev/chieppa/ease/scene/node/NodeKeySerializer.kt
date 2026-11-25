package dev.chieppa.ease.scene.node

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.SerializedAs
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Writes a [NodeKey] as `{path}`, its text in one field: an object, so a key can travel as a
 * component inside a snapshot, where every value names its type.
 */
@SerializedAs(NodeKeyData::class)
internal object NodeKeySerializer : KSerializer<NodeKey> {
  override val descriptor: SerialDescriptor = NodeKeyData.serializer().descriptor

  override fun serialize(encoder: Encoder, value: NodeKey) {
    encoder.encodeSerializableValue(NodeKeyData.serializer(), NodeKeyData(value.text))
  }

  override fun deserialize(decoder: Decoder): NodeKey =
    NodeKey.parse(decoder.decodeSerializableValue(NodeKeyData.serializer()).path)
}

@Doc("A node's key as written: its ids' hex joined by /.")
@Serializable
@SerialName("ease.NodeKey")
private class NodeKeyData(
  @Doc("The placement ids, outermost first, then the node's own id, joined by /.") val path: String
)
