package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.replication
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sets the [Velocity] of the entity [entity] names to ([x], [y]); undone by setting the previous
 * velocity back. Rejected unless the world holds authority over it.
 */
@Serializable
@SerialName("sample.Steer")
public class Steer(public val entity: NetworkId, public val x: Float, public val y: Float) :
  Command {
  override fun apply(world: World): Command {
    val target = world.entityOf(entity)
    val steerable = target != Entity.NONE && with(world) { target.has(Velocity) }
    if (!steerable) throw CommandRejected("Entity $entity has no Velocity to steer")
    if (!world.replication.hasAuthority(target)) {
      throw CommandRejected("Entity $entity is held by another peer")
    }
    val velocity = with(world) { target[Velocity] }
    val previous = Steer(entity, velocity.x, velocity.y)
    world.replication.write(target, Velocity) {
      this.x = this@Steer.x
      this.y = this@Steer.y
    }
    return previous
  }
}
