package dev.chieppa.ease.scene.commands

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.wildcardSnapshotOf
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandScope
import dev.chieppa.ease.commands.builtin.rejectIf
import dev.chieppa.ease.scene.node.NodeKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Changes a node's scene components: sets each component in the snapshot, adding or replacing " +
    "it, and removes the named ones. Undone by setting back what it replaced and removing what " +
    "it added. Only the world's own peer may send it; rejected unless the world holds " +
    "authority over the node and knows every type."
)
@Serializable
@SerialName("ease.UpdateNode")
public class UpdateNode(
  @Doc("The key of the node to change.") public val node: NodeKey,
  @Doc("The components to add or replace; its tags are ignored.") public val set: Snapshot,
  @Doc("The serial names of the scene components to remove.") public val remove: List<String>,
) : Command {
  override val scope: CommandScope
    get() = CommandScope.LOCAL

  override fun validate(world: World): String? = heldNodeRefusal(world, node) ?: typeRefusal(world)

  /** Why a type this command names is unknown to [world]'s scenes, or null when all are known. */
  private fun typeRefusal(world: World): String? {
    val types = sceneNodes(world).types
    val unknownSet = set.components.firstOrNull { types.of(it) == null }
    val unknownRemove = remove.firstOrNull { types[it] == null }
    return when {
      unknownSet != null -> "$unknownSet is not a scene component type"
      unknownRemove != null -> "No scene component type is named $unknownRemove"
      else -> null
    }
  }

  override fun apply(world: World): Command {
    rejectIf(validate(world))
    val nodes = sceneNodes(world)
    val entity = nodes.index[node]
    val setting = set.components.map { checkNotNull(nodes.types.of(it)) }
    val removing = remove.map { checkNotNull(nodes.types[it]) }
    val restore = mutableListOf<Component<*>>()
    val unset = mutableListOf<String>()
    setting.forEach { type ->
      val previous = type.of(world, entity)
      if (previous == null) unset += type.serialName else restore += type.copyOf(previous)
    }
    removing.forEach { type -> type.of(world, entity)?.let { restore += type.copyOf(it) } }
    val added =
      set.components.filterIndexed { index, component ->
        !setting[index].writeReplicated(world, entity, component)
      }
    with(world) {
      entity.configure { updated ->
        removing.forEach { type ->
          if (type.of(world, updated) != null) type.removeFrom(this, updated)
        }
        updated += added
      }
    }
    added.forEach { component -> nodes.types.of(component)?.markReplicated(world, entity) }
    return UpdateNode(node, wildcardSnapshotOf(restore, emptyList()), unset)
  }
}
