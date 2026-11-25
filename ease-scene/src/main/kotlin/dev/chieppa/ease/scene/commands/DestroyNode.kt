package dev.chieppa.ease.scene.commands

import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandScope
import dev.chieppa.ease.commands.builtin.rejectIf
import dev.chieppa.ease.commands.builtin.withoutIdentity
import dev.chieppa.ease.scene.node.NodeKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Removes a node and everything it carries; its children stay. Undone by spawning it again " +
    "with the same components. Only the world's own peer may send it; rejected unless the " +
    "world holds authority over the node."
)
@Serializable
@SerialName("ease.DestroyNode")
public class DestroyNode(@Doc("The key of the node to remove.") public val node: NodeKey) :
  Command {
  override val scope: CommandScope
    get() = CommandScope.LOCAL

  override fun validate(world: World): String? = heldNodeRefusal(world, node)

  override fun apply(world: World): Command {
    rejectIf(validate(world))
    val entity = sceneNodes(world).index[node]
    val snapshot = withoutIdentity(world.snapshotOf(entity))
    world -= entity
    return SpawnNode(snapshot)
  }
}
