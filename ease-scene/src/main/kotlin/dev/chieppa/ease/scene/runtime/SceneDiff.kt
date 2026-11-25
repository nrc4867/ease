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
 * one the last applied version of the scene had ([previous], parents first) and this one lacks is
 * destroyed. Only the component types in [SceneNodes.types] are compared; components of other
 * types, and those that failed to load this time, are left as they are. Spawns and updates come
 * parents first, then removals children first.
 */
internal class SceneDiff(private val world: World, private val nodes: SceneNodes) {
  fun commands(scene: ResolvedScene, previous: List<NodeKey>): List<Command> {
    val spawns = mutableListOf<Command>()
    val updates = mutableListOf<Command>()
    for (node in scene.nodes) {
      val entity = nodes.index[node.key]
      if (entity == Entity.NONE) spawns += spawn(node) else update(node, entity)?.let(updates::add)
    }
    val gone = previous.asReversed().filter { scene[it] == null && it in nodes.index }
    val removals = gone.map { DestroyNode(it) }
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
      if (type.serialName in node.failed) continue
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
