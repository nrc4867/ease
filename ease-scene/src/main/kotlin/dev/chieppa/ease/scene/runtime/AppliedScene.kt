package dev.chieppa.ease.scene.runtime

import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.ResolvedNode
import dev.chieppa.ease.scene.resolve.ResolvedScene
import java.util.TreeMap

/**
 * The version of this resolved scene that counts as applied once its changes ran: a component that
 * failed to load keeps the value [previous] had (its last good one), and a node whose change was
 * rejected, one of [rejected], stays as [previous] had it, or absent when [previous] lacked it, so
 * the change is tried again on the next reload.
 */
internal fun ResolvedScene.appliedAfter(
  previous: ResolvedScene?,
  rejected: Set<NodeKey>,
): ResolvedScene {
  val kept = nodes.mapNotNull { node ->
    if (node.key in rejected) previous?.get(node.key) else node.carrying(previous?.get(node.key))
  }
  val stayed = previous?.nodes.orEmpty().filter { it.key in rejected && this[it.key] == null }
  return ResolvedScene(kept + stayed, assets, problems)
}

private fun ResolvedNode.carrying(before: ResolvedNode?): ResolvedNode {
  if (failed.isEmpty() || before == null) return this
  val components = TreeMap(components)
  failed.forEach { name -> before.components[name]?.let { components[name] = it } }
  return ResolvedNode(key, components, failed)
}
