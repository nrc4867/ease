package dev.chieppa.ease.scene.document

import dev.chieppa.ease.scene.node.KEY_SEPARATOR
import dev.chieppa.ease.scene.node.LocalId

/** The fewest parts an override path's text has: one id, a component and a field. */
private const val MIN_PARTS = 3

/**
 * What a prefab instance override changes: the field [field] of the component [component] (its
 * serial name) on the node [nodes] names inside the prefab. [nodes] is one local id for a node of
 * the prefab itself, or a path of ids into an instance nested in it. Written as
 * `<ids>/<component>/<field>`, such as `3f9a0c12b4e5d678/ease.LocalTransform/x`; paths sort by that
 * text, as the file's keys do.
 */
public class OverridePath(
  public val nodes: List<LocalId>,
  public val component: String,
  public val field: String,
) : Comparable<OverridePath> {
  init {
    require(nodes.isNotEmpty()) { "An override path names at least one node" }
    require(KEY_SEPARATOR !in component && KEY_SEPARATOR !in field) {
      "Component and field names hold no $KEY_SEPARATOR"
    }
  }

  /** The path as written in the instance node's file. */
  public val text: String =
    (nodes.map { it.text } + component + field).joinToString(KEY_SEPARATOR.toString())

  /** This path seen from inside the instance its first node is, or null for a single node. */
  internal fun inner(): OverridePath? =
    if (nodes.size == 1) null else OverridePath(nodes.drop(1), component, field)

  override fun compareTo(other: OverridePath): Int = text.compareTo(other.text)

  override fun equals(other: Any?): Boolean = other is OverridePath && other.text == text

  override fun hashCode(): Int = text.hashCode()

  override fun toString(): String = text

  public companion object {
    /** The path [text] spells. Throws [IllegalArgumentException] when it is not one. */
    public fun parse(text: String): OverridePath {
      val parts = text.split(KEY_SEPARATOR)
      require(parts.size >= MIN_PARTS) { "An override path is <ids>/<component>/<field>: $text" }
      val ids = parts.subList(0, parts.size - 2).map { LocalId.parse(it) }
      return OverridePath(ids, parts[parts.size - 2], parts.last())
    }
  }
}
