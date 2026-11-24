package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.commands.CommandScope
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.replication
import kotlin.math.abs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Sets the Velocity of an entity; undone by setting the previous velocity back. A peer may " +
    "steer only entities it owns; rejected unless the world holds authority over the entity."
)
@Serializable
@SerialName("sample.Steer")
public class Steer(
  @Doc("The entity to steer.") public val entity: NetworkId,
  @Doc("Speed along x, in world units a second.")
  @Range(-Velocity.MAX_SPEED, Velocity.MAX_SPEED)
  public val x: Float,
  @Doc("Speed along y, in world units a second.")
  @Range(-Velocity.MAX_SPEED, Velocity.MAX_SPEED)
  public val y: Float,
) : Command {
  override val scope: CommandScope
    get() = CommandScope.OWNED_ENTITIES

  override fun targets(): List<NetworkId> = listOf(entity)

  override fun validate(world: World): String? {
    val target = world.entityOf(entity)
    return when {
      target == Entity.NONE || !with(world) { target.has(Velocity) } ->
        "Entity $entity has no Velocity to steer"
      !world.replication.hasAuthority(target) -> "Entity $entity is held by another peer"
      abs(x) > Velocity.MAX_SPEED || abs(y) > Velocity.MAX_SPEED ->
        "A speed is at most ${Velocity.MAX_SPEED} along each axis"
      else -> null
    }
  }

  override fun apply(world: World): Command {
    validate(world)?.let { throw CommandRejected(it) }
    val target = world.entityOf(entity)
    val velocity = with(world) { target[Velocity] }
    val previous = Steer(entity, velocity.x, velocity.y)
    world.replication.write(target, Velocity) {
      this.x = this@Steer.x
      this.y = this@Steer.y
    }
    return previous
  }
}
