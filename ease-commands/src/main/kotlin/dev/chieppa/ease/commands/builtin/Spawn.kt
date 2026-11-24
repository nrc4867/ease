package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.spawnEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Creates an entity carrying what [snapshot] holds, under a network id the world issues; undone by
 * destroying it. The world's own peer holds it; an `Authority` in [snapshot] may name another
 * owner, but not another holder.
 */
@Serializable
@SerialName("ease.Spawn")
public class Spawn(public val snapshot: Snapshot) : Command {
  /** Spawns an entity carrying [components] and no tags. */
  public constructor(components: List<Component<out Any>>) : this(Snapshot(components, emptyList()))

  override fun apply(world: World): Command {
    snapshotRefusal(world, snapshot)?.let { throw CommandRejected(it) }
    val entity = world.spawnEntity { it += snapshot.components }
    return Destroy(world.networkIdOf(entity))
  }
}
