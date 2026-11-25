package dev.chieppa.ease.scene.files

import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId

/** The fewest hex digits of the local id a file name carries. */
private const val SHORT_ID_DIGITS = 4

/** The longest name part of a file name, in characters. */
private const val MAX_SLUG = 40

private const val SEPARATOR = "-"

private val NOT_SLUG = Regex("[^a-z0-9]+")

/**
 * How node files are named, so a pull request shows which node changed: the node's name as a
 * lowercase slug, a dash, and the first hex digits of its local id, such as `lamp-7f3a`. A node
 * with no name, or none that slugs to anything, is its short id alone. The short id has four
 * digits, more only when another node of the folder already has the shorter name. Names only help
 * people: reading never depends on them, since every file holds its node's full id.
 */
public object NodeFileNames {
  /** [name] as a file name part: lowercase letters and digits, other runs as one dash. */
  public fun slug(name: String): String {
    val dashed = name.lowercase().replace(NOT_SLUG, SEPARATOR).trim('-')
    return dashed.take(MAX_SLUG).trimEnd('-')
  }

  /** The file name, without extension, of [node] with a short id of [digits] hex digits. */
  public fun stem(node: SceneNode, digits: Int = SHORT_ID_DIGITS): String {
    val short = node.id.text.take(digits)
    val slug = slug(node.name)
    return if (slug.isEmpty()) short else slug + SEPARATOR + short
  }

  /** Whether [stem] follows the rule for [node], with a short id of any allowed length. */
  public fun fits(stem: String, node: SceneNode): Boolean =
    (SHORT_ID_DIGITS..node.id.text.length).any { stem(node, it) == stem }

  /**
   * A file name, without extension, for every node of [document]. A node whose name in [previous]
   * still follows the rule keeps it, so a save renames only renamed nodes; every other node takes
   * the shortest name no other node of the folder has, in id order.
   */
  public fun assign(document: SceneDocument, previous: Map<LocalId, String>): Map<LocalId, String> {
    val names = HashMap<LocalId, String>()
    val taken = HashSet<String>()
    for (node in document.nodes.values) {
      val kept = previous[node.id]?.takeIf { fits(it, node) && it !in taken } ?: continue
      names[node.id] = kept
      taken += kept
    }
    for (node in document.nodes.values) {
      if (node.id in names) continue
      val stem =
        (SHORT_ID_DIGITS..node.id.text.length).map { stem(node, it) }.first { it !in taken }
      names[node.id] = stem
      taken += stem
    }
    return names
  }
}
