package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Removes [entity] and everything it carries; undone by restoring it under the same id. */
@Serializable
@SerialName("ease.Destroy")
public class Destroy(public val entity: Entity) : Command {
  override fun apply(world: World): Command {
    if (entity !in world) throw CommandRejected("Entity ${entity.id} does not exist")
    val snapshot = world.snapshotOf(entity)
    world -= entity
    return Restore(entity, snapshot)
  }
}
