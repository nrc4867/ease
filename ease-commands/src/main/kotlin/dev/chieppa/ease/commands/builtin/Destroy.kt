package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.entityOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Removes the entity and everything it carries; undone by restoring it under the same network " +
    "id. Rejected unless the world holds authority over it."
)
@Serializable
@SerialName("ease.Destroy")
public class Destroy(@Doc("The entity to remove.") public val entity: NetworkId) : Command {
  override fun targets(): List<NetworkId> = listOf(entity)

  override fun validate(world: World): String? = heldRefusal(world, entity)

  override fun apply(world: World): Command {
    rejectIf(validate(world))
    val target = world.entityOf(entity)
    val snapshot = withoutIdentity(world.snapshotOf(target))
    world -= target
    return Restore(entity, snapshot)
  }
}
