package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.spawnEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Creates an entity carrying what the snapshot holds, under a network id the world issues; " +
    "undone by destroying it. The world's own peer holds it; an Authority in the snapshot may " +
    "name another owner, but not another holder."
)
@Serializable
@SerialName("ease.Spawn")
public class Spawn(
  @Doc("The components and tags the new entity carries.") public val snapshot: Snapshot
) : Command {
  /** Spawns an entity carrying [components] and no tags. */
  public constructor(components: List<Component<out Any>>) : this(Snapshot(components, emptyList()))

  override fun validate(world: World): String? = snapshotRefusal(world, snapshot)

  override fun apply(world: World): Command {
    rejectIf(validate(world))
    val entity = world.spawnEntity { it += snapshot.components }
    return Destroy(world.networkIdOf(entity))
  }
}
