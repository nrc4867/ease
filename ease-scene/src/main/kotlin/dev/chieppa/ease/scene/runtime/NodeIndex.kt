package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.scene.node.NodeKey

/**
 * The live entity of each node key in one world, kept by the `NodeKey` component itself as it is
 * added and removed. Looking a key up never allocates. Commands refuse a second entity with a live
 * key before creating it; should one be made outside them, the first keeps the key.
 */
public class NodeIndex internal constructor() {
  private val entities = HashMap<NodeKey, Entity>()

  /** How many nodes are live. */
  public val size: Int
    get() = entities.size

  /** Every live node's key, in no particular order. */
  public val keys: Set<NodeKey>
    get() = entities.keys

  /** The live entity of [key], or [Entity.NONE] when no node has it. */
  public operator fun get(key: NodeKey): Entity = entities[key] ?: Entity.NONE

  /** Whether a live node has [key]. */
  public operator fun contains(key: NodeKey): Boolean = entities.containsKey(key)

  internal fun put(key: NodeKey, entity: Entity) {
    entities.putIfAbsent(key, entity)
  }

  internal fun remove(key: NodeKey, entity: Entity) {
    entities.remove(key, entity)
  }
}
