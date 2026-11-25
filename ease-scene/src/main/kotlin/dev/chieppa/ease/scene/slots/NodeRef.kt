package dev.chieppa.ease.scene.slots

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.scene.node.NodeKey
import kotlinx.serialization.Serializable

/** The serial name of node slots, which resolving a scene looks for. */
public const val NODE_REF_NAME: String = "ease.NodeRef"

/**
 * A node slot: a component field naming another node, such as a target or a spawn point. In a scene
 * or prefab file it holds the local id of a node of the same file, or a path of ids into an
 * instance placed there (`instance/inner`); spawning resolves it to the full [key], so copies of
 * one prefab each point at their own node.
 */
@Doc(
  "A node slot: in a file, the local id of a node in the same scene or prefab (or ids joined by " +
    "/ into an instance placed there); at runtime, the node's full key."
)
@Serializable(with = NodeRefSerializer::class)
public class NodeRef(@Doc("The key of the node the slot names.") public val key: NodeKey) {
  override fun equals(other: Any?): Boolean = other is NodeRef && other.key == key

  override fun hashCode(): Int = key.hashCode()

  override fun toString(): String = "NodeRef(${key.text})"
}
