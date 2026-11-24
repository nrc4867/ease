package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sets [entity]'s [Velocity] to ([x], [y]); undone by setting the previous velocity back. */
@Serializable
@SerialName("sample.Steer")
public class Steer(public val entity: Entity, public val x: Float, public val y: Float) : Command {
  override fun apply(world: World): Command =
    with(world) {
      if (entity !in world || !entity.has(Velocity)) {
        throw CommandRejected("Entity ${entity.id} has no Velocity to steer")
      }
      val velocity = entity[Velocity]
      val previous = Steer(entity, velocity.x, velocity.y)
      velocity.x = x
      velocity.y = y
      previous
    }
}
