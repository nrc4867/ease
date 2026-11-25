package dev.chieppa.ease.scene.resolve

import dev.chieppa.ease.scene.node.NodeKey

/**
 * Puts nodes parents first: a walk down the tree from the nodes whose parent is not among them,
 * siblings in key order. Nodes no walk reaches, those in a parent loop, come last in key order, so
 * every node appears once.
 */
internal object TreeOrder {
  /** The keys of [parents] (each node's parent, or null) with every parent before its children. */
  fun of(parents: Map<NodeKey, NodeKey?>): List<NodeKey> {
    val byText = compareBy<NodeKey> { it.text }
    val children = HashMap<NodeKey, MutableList<NodeKey>>()
    val roots = mutableListOf<NodeKey>()
    parents.forEach { (key, parent) ->
      if (parent == null || parent !in parents) roots += key
      else children.getOrPut(parent) { mutableListOf() } += key
    }
    val ordered = LinkedHashSet<NodeKey>()
    val pending = ArrayDeque(roots.sortedWith(byText.reversed()))
    while (pending.isNotEmpty()) {
      val key = pending.removeLast()
      if (ordered.add(key)) {
        children[key]?.sortedWith(byText.reversed())?.forEach { pending.addLast(it) }
      }
    }
    parents.keys.sortedWith(byText).forEach { ordered.add(it) }
    return ordered.toList()
  }
}
