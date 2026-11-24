package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Creates [entity] under its own id with exactly what [snapshot] holds, as undoing a [Destroy]
 * does; undone by destroying it again.
 */
@Serializable
@SerialName("ease.Restore")
public class Restore(public val entity: Entity, public val snapshot: Snapshot) : Command {
  override fun apply(world: World): Command {
    if (entity in world) throw CommandRejected("Entity ${entity.id} already exists")
    world.loadSnapshotOf(entity, snapshot)
    return Destroy(entity)
  }
}
