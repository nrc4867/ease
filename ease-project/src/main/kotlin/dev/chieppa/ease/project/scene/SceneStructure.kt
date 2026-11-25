package dev.chieppa.ease.project.scene

import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey

/** Facts about the shape of a scene's node tree that its files can get wrong after a merge. */
public object SceneStructure {
  /** The nodes that are each other's ancestors, one list per loop, in id order. */
  public fun parentCycles(document: SceneDocument): List<List<LocalId>> {
    val reported = HashSet<LocalId>()
    val cycles = mutableListOf<List<LocalId>>()
    document.nodes.keys.forEach { start ->
      if (start in reported) return@forEach
      val walked = LinkedHashSet<LocalId>()
      var current: LocalId? = start
      while (current != null && walked.add(current)) current = document[current]?.parent
      if (current != null) {
        val loop = walked.toList().dropWhile { it != current }.sorted()
        if (reported.addAll(loop)) cycles += loop
      }
    }
    return cycles
  }

  /** Groups of siblings that share an order key, each group in id order. */
  public fun equalOrderKeys(document: SceneDocument): List<List<SceneNode>> =
    document.nodes.values
      .groupBy { it.parent to it.order }
      .values
      .filter { it.size > 1 }
      .sortedBy { it.first().id }

  /**
   * The order keys that make every group of [equalOrderKeys] unique: the first node of a run keeps
   * its key and the others get keys between it and the next sibling's, so the siblings keep their
   * order and few files change. Maps the nodes that change to their new key.
   */
  public fun respaced(document: SceneDocument): Map<LocalId, OrderKey> {
    val changed = HashMap<LocalId, OrderKey>()
    document.nodes.values
      .groupBy { it.parent }
      .values
      .forEach { siblings ->
        val sorted = siblings.sortedWith(compareBy({ it.order }, { it.id }))
        var index = 0
        while (index < sorted.size) {
          val key = sorted[index].order
          val run = sorted.drop(index).takeWhile { it.order == key }
          val next = sorted.getOrNull(index + run.size)?.order
          var last = key
          run.drop(1).forEach { node ->
            last = OrderKey.between(last, next)
            changed[node.id] = last
          }
          index += run.size
        }
      }
    return changed
  }
}
