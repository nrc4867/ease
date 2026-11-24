package dev.chieppa.ease.scene.node

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.scene.runtime.SceneNodes
import kotlinx.serialization.Serializable

/** Separates the ids of a key's text. */
internal const val KEY_SEPARATOR = '/'

/**
 * Which node an entity is, stable across reloads, saves and peers: the local ids of the placements
 * that led to the node (sublevel and prefab instance nodes, outermost first), then the node's own
 * local id. Two copies of one prefab or sublevel never collide, because their placement ids differ.
 * Written as the ids' hex joined by `/`. Equal keys have equal ids; comparing or hashing a key
 * never allocates. A world with a [SceneNodes] system indexes every entity carrying one.
 */
@Doc(
  "Which node an entity is: the local ids of the prefab and sublevel placements that led to it, " +
    "outermost first, then its own local id, written as hex joined by /."
)
@Serializable(with = NodeKeySerializer::class)
public class NodeKey private constructor(private val ids: LongArray) : Component<NodeKey> {
  private val hash = ids.contentHashCode()

  /** The node's own local id, the last of the key. */
  public val id: LocalId
    get() = LocalId(ids.last())

  /** How many ids the key holds: 1 for a node of the outermost scene. */
  public val depth: Int
    get() = ids.size

  /** The placement ids before the node's own, outermost first. */
  public val placement: List<LocalId>
    get() = List(ids.size - 1) { LocalId(ids[it]) }

  /** The key of the node [child] inside the placement this node makes. */
  public fun child(child: LocalId): NodeKey = NodeKey(ids + child.value)

  /** Whether this key is [prefix] or lies inside the placement [prefix] makes. */
  public fun startsWith(prefix: NodeKey): Boolean =
    prefix.ids.size <= ids.size && prefix.ids.indices.all { ids[it] == prefix.ids[it] }

  /** The key's text: each id's hex, joined by `/`. */
  public val text: String
    get() = ids.joinToString(KEY_SEPARATOR.toString()) { LocalId(it).text }

  override fun equals(other: Any?): Boolean =
    other is NodeKey && other.hash == hash && other.ids.contentEquals(ids)

  override fun hashCode(): Int = hash

  override fun toString(): String = text

  override fun type(): ComponentType<NodeKey> = NodeKey

  override fun World.onAdd(entity: Entity) {
    systemOrNull<SceneNodes>()?.index?.put(this@NodeKey, entity)
  }

  override fun World.onRemove(entity: Entity) {
    systemOrNull<SceneNodes>()?.index?.remove(this@NodeKey, entity)
  }

  /** The component type, and the ways to make a key. */
  public companion object : ComponentType<NodeKey>() {
    /** The key of the node [id] inside the placements [placement], outermost first. */
    public fun of(placement: List<LocalId>, id: LocalId): NodeKey =
      NodeKey(
        LongArray(placement.size + 1) { if (it < placement.size) placement[it].value else id.value }
      )

    /** The key of the node [id] of the outermost scene. */
    public fun of(id: LocalId): NodeKey = NodeKey(longArrayOf(id.value))

    /** The key [text] spells. Throws [IllegalArgumentException] when it is not a key. */
    public fun parse(text: String): NodeKey {
      val parts = text.split(KEY_SEPARATOR)
      return NodeKey(LongArray(parts.size) { LocalId.parse(parts[it]).value })
    }
  }
}
