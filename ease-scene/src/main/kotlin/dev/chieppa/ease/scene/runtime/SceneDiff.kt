package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.wildcardSnapshotOf
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.scene.commands.DestroyNode
import dev.chieppa.ease.scene.commands.SpawnNode
import dev.chieppa.ease.scene.commands.UpdateNode
import dev.chieppa.ease.scene.resolve.ResolvedNode
import dev.chieppa.ease.scene.resolve.ResolvedScene
import kotlinx.serialization.json.JsonObject

/**
 * The commands that bring a world's live nodes in line with the files, matched by key and compared
 * with [previous], the version of the scene last applied, not with the live nodes: a node the scene
 * has and the world lacks spawns; on a live node, a component whose file value changed since
 * [previous] is written field by field, only the fields that changed, so the rest keep their live
 * values; a component the file gained is set whole and one it lost is removed; a node [previous]
 * had and this version lacks is destroyed. Fields and components the files did not change are left
 * as running systems made them. Only the component types in [SceneNodes.types] are compared;
 * components of other types, and those that failed to load this time, are left as they are. Spawns
 * and updates come parents first, then removals children first.
 */
internal class SceneDiff(private val world: World, private val nodes: SceneNodes) {
  fun commands(scene: ResolvedScene, previous: ResolvedScene?): List<Command> {
    val spawns = mutableListOf<Command>()
    val updates = mutableListOf<Command>()
    for (node in scene.nodes) {
      val entity = nodes.index[node.key]
      if (entity == Entity.NONE) {
        spawns += spawn(node)
      } else {
        update(node, previous?.get(node.key), entity)?.let(updates::add)
      }
    }
    val gone =
      previous
        ?.nodes
        .orEmpty()
        .asReversed()
        .map { it.key }
        .filter {
          scene[it] == null && it in nodes.index
        }
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

  private fun update(node: ResolvedNode, before: ResolvedNode?, entity: Entity): Command? {
    val set = mutableListOf<Component<*>>()
    val remove = mutableListOf<String>()
    for (type in nodes.types.all.filter { it.serialName !in node.failed }) {
      val wanted = node.components[type.serialName]
      val was = before?.components?.get(type.serialName)
      val live = type.of(world, entity)?.let { type.encode(it) }
      when {
        wanted == was -> Unit
        wanted == null -> if (live != null) remove += type.serialName
        else -> merge(live, was, wanted).takeIf { it != live }?.let { set += type.decode(it) }
      }
    }
    if (set.isEmpty() && remove.isEmpty()) return null
    return UpdateNode(node.key, wildcardSnapshotOf(set, emptyList()), remove)
  }

  /**
   * [live] with the fields of [wanted] that differ from [was] written; [wanted] when none lives.
   */
  private fun merge(live: JsonObject?, was: JsonObject?, wanted: JsonObject): JsonObject {
    if (live == null || was == null) return wanted
    val fields = LinkedHashMap(live)
    for ((name, value) in wanted) if (was[name] != value) fields[name] = value
    for (name in was.keys) if (name !in wanted) fields.remove(name)
    return JsonObject(fields)
  }
}
