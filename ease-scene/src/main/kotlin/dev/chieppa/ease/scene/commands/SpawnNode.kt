package dev.chieppa.ease.scene.commands

import com.github.quillraven.fleks.EntityTags
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandScope
import dev.chieppa.ease.commands.builtin.rejectIf
import dev.chieppa.ease.ecs.replication.spawnEntity
import dev.chieppa.ease.scene.node.NodeKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Creates a node: an entity carrying what the snapshot holds, its NodeKey included, with a " +
    "network id and authority from the world; its replicated components are marked for the next " +
    "snapshot. Undone by destroying the node. Only the world's " +
    "own peer may send it; rejected when a live node has the key."
)
@Serializable
@SerialName("ease.SpawnNode")
public class SpawnNode(
  @Doc("The components and tags of the new node, one NodeKey among them.")
  public val snapshot: Snapshot
) : Command {
  override val scope: CommandScope
    get() = CommandScope.LOCAL

  override fun validate(world: World): String? = spawnRefusal(world, snapshot)

  override fun apply(world: World): Command {
    rejectIf(validate(world))
    @Suppress("UNCHECKED_CAST") // Fleks keeps every tag as a UniqueId of Any.
    val tags = snapshot.tags as List<EntityTags>
    val entity = world.spawnEntity {
      it += snapshot.components
      it += tags
    }
    val types = sceneNodes(world).types
    snapshot.components.forEach { types.of(it)?.markReplicated(world, entity) }
    return DestroyNode(snapshot.components.filterIsInstance<NodeKey>().single())
  }
}
