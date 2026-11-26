package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.scene.node.LocalId
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * The local ids of an imported prefab's nodes: the first 64 bits of the SHA-1 of the node's place
 * in the glTF file (`node 3`, or `node 3 primitive 1` for the extra nodes a mesh of several
 * primitives gets), so every import of the same file gives every node the same id, and overrides on
 * placed copies keep finding it.
 */
internal object NodeIds {
  /** The id of glTF node [node]. */
  fun node(node: Int): LocalId = of("node $node")

  /** The id of the node drawing primitive [primitive] of glTF node [node]'s mesh. */
  fun primitive(node: Int, primitive: Int): LocalId = of("node $node primitive $primitive")

  private fun of(text: String): LocalId {
    val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
    return LocalId(ByteBuffer.wrap(digest).getLong(0))
  }
}
