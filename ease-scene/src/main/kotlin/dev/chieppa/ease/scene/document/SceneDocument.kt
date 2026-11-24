package dev.chieppa.ease.scene.document

import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import java.util.SortedMap
import kotlinx.serialization.json.JsonObject

/**
 * A scene or prefab as its folder holds it: the scene-wide [settings] and every node, by id. Nodes
 * link to their parent and carry their own order key; no node lists its children, which [children]
 * works out. A document is a value: every change returns a new one, which `SceneFolder.write` saves
 * by rewriting only the files that differ.
 */
public class SceneDocument(
  public val settings: JsonObject = JsonObject(emptyMap()),
  nodes: Collection<SceneNode> = emptyList(),
) {
  /** Every node by id, in id order. */
  public val nodes: SortedMap<LocalId, SceneNode> = nodes.associateBy { it.id }.toSortedMap()

  init {
    require(this.nodes.size == nodes.size) { "Two nodes share a local id" }
  }

  /** The node [id], or null when there is none. */
  public operator fun get(id: LocalId): SceneNode? = nodes[id]

  /** The children of [parent] (null for the top nodes), by order key, then by id. */
  public fun children(parent: LocalId?): List<SceneNode> =
    nodes.values.filter { it.parent == parent }.sortedWith(compareBy({ it.order }, { it.id }))

  /** An order key that puts a new child of [parent] after its last child. */
  public fun nextOrder(parent: LocalId?): OrderKey =
    OrderKey.after(children(parent).lastOrNull()?.order)

  /** This document with [node] added, or replacing the node with its id. */
  public fun with(node: SceneNode): SceneDocument =
    SceneDocument(settings, nodes.values.filter { it.id != node.id } + node)

  /** This document without the node [id]; its children are left as they are. */
  public fun without(id: LocalId): SceneDocument =
    SceneDocument(settings, nodes.values.filter { it.id != id })

  override fun equals(other: Any?): Boolean =
    other is SceneDocument && other.settings == settings && other.nodes == nodes

  override fun hashCode(): Int = nodes.hashCode()
}
