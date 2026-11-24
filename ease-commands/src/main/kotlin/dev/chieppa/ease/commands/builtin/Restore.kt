package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityTags
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.entityOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Creates an entity under the network id [entity] with exactly what [snapshot] holds, as undoing a
 * [Destroy] does; undone by destroying it again. The Fleks entity may differ, the network id never.
 */
@Serializable
@SerialName("ease.Restore")
public class Restore(public val entity: NetworkId, public val snapshot: Snapshot) : Command {
  override fun apply(world: World): Command {
    refusal(world)?.let { throw CommandRejected(it) }
    @Suppress("UNCHECKED_CAST") // Fleks keeps every tag as a UniqueId of Any.
    val tags = snapshot.tags as List<EntityTags>
    world.entity {
      it += NetworkIdentity(entity)
      it += snapshot.components
      it += tags
    }
    return Destroy(entity)
  }

  /** Why this cannot apply to [world], or null when it can. */
  private fun refusal(world: World): String? =
    when {
      entity.value <= 0L -> "Network ids start at 1, not ${entity.value}"
      world.entityOf(entity) != Entity.NONE -> "Entity $entity already exists"
      snapshot.components.any { it is NetworkIdentity } ->
        "A restored entity takes its id from the command, not its components"
      else -> null
    }
}
