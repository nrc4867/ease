package dev.chieppa.ease.serialization.merge

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * A three-way merge of two edited trees of one base, key by key through nested mappings. A value
 * only one side edited takes that side's; a value both sides edited alike stays. A value the two
 * sides edited differently, or that one side deleted and the other edited, is a conflict and is
 * never settled by picking a side. Lists and scalars count as whole values.
 *
 * Unlike a line merge, two sides that add different keys at the same spot, or change neighbouring
 * keys, merge cleanly.
 */
public object TreeMerge {
  /**
   * What merging gave: the merged [tree] (null when the whole value is absent), and the
   * [conflicts], the paths of the values the sides disagree on, `/` between keys. With conflicts,
   * the tree holds our side's value at each conflicting path.
   */
  public class Outcome(public val tree: JsonElement?, public val conflicts: List<String>) {
    /** Whether the merge settled every value. */
    public val clean: Boolean
      get() = conflicts.isEmpty()
  }

  /** Merges [ours] and [theirs], both edited from [base]; null stands for absent. */
  public fun merge(base: JsonElement?, ours: JsonElement?, theirs: JsonElement?): Outcome {
    val conflicts = mutableListOf<String>()
    val tree = merge(base, ours, theirs, "", conflicts)
    return Outcome(tree, conflicts)
  }

  private fun merge(
    base: JsonElement?,
    ours: JsonElement?,
    theirs: JsonElement?,
    path: String,
    conflicts: MutableList<String>,
  ): JsonElement? =
    when {
      ours == theirs -> ours
      ours == base -> theirs
      theirs == base -> ours
      ours is JsonObject && theirs is JsonObject ->
        mergeMappings(base as? JsonObject, ours, theirs, path, conflicts)
      else -> {
        conflicts += path.ifEmpty { "/" }
        ours
      }
    }

  private fun mergeMappings(
    base: JsonObject?,
    ours: JsonObject,
    theirs: JsonObject,
    path: String,
    conflicts: MutableList<String>,
  ): JsonElement {
    val merged = linkedMapOf<String, JsonElement>()
    (ours.keys + theirs.keys + base?.keys.orEmpty()).toSortedSet().forEach { key ->
      val value =
        merge(base?.get(key), ours[key], theirs[key], "$path/$key".removePrefix("/"), conflicts)
      if (value != null) merged[key] = value
    }
    return JsonObject(merged)
  }
}
