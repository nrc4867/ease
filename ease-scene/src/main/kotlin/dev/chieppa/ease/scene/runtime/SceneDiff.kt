package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.wildcardSnapshotOf
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.scene.commands.DestroyNode
import dev.chieppa.ease.scene.commands.SpawnNode
import dev.chieppa.ease.scene.commands.UpdateNode
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.ResolvedNode
import dev.chieppa.ease.scene.resolve.ResolvedScene

/**
 * The commands that bring a world's live nodes in line with a resolved scene, matched by key: a
 * node the scene has and the world lacks spawns, one whose scene components differ is updated, and
 * one the last applied version of the scene had ([previous]) and this one lacks is destroyed. Only
 * the component types in [SceneNodes.types] are compared; components of other types are left alone.
 * Spawns come in key order, then updates, then removals.
 */
internal class SceneDiff(private val world: World, private val nodes: SceneNodes) {
  fun commands(scene: ResolvedScene, previous: Collection<NodeKey>): List<Command> {
    val spawns = mutableListOf<Command>()
    val updates = mutableListOf<Command>()
    for (node in scene.nodes) {
      val entity = nodes.index[node.key]
      if (entity == Entity.NONE) spawns += spawn(node) else update(node, entity)?.let(updates::add)
    }
    val removals =
      previous
        .filter { scene[it] == null && it in nodes.index }
        .sortedByDescending { it.text }
        .map { DestroyNode(it) }
    return spawns + updates + removals
  }

  private fun spawn(node: ResolvedNode): Command {
    val components = mutableListOf<Component<*>>(node.key)
    node.components.forEach { (name, fields) ->
      nodes.types[name]?.let { components += it.decode(fields) }
    }
    return SpawnNode(wildcardSnapshotOf(components, emptyList()))
  }

  private fun update(node: ResolvedNode, entity: Entity): Command? {
    val set = mutableListOf<Component<*>>()
    val remove = mutableListOf<String>()
    for (type in nodes.types.all) {
      val wanted = node.components[type.serialName]
      val live = type.of(world, entity)?.let { type.encode(it) }
      when {
        wanted == null && live != null -> remove += type.serialName
        wanted != null && wanted != live -> set += type.decode(wanted)
      }
    }
    if (set.isEmpty() && remove.isEmpty()) return null
    return UpdateNode(node.key, wildcardSnapshotOf(set, emptyList()), remove)
  }
}
