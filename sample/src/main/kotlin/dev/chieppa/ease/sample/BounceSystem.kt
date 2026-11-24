package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.replication

/**
 * Moves every entity with a [Velocity] by it each tick, and turns it back along an axis once its
 * center passes the [Playfield] on that axis. It writes only entities this world holds authority
 * over, through [Replication.write].
 */
public class BounceSystem : IteratingSystem(family { all(LocalTransform, Velocity) }) {
  private val replication: Replication = world.replication

  override fun onTickEntity(entity: Entity) {
    if (!replication.hasAuthority(entity)) return
    val velocity = entity[Velocity]
    replication.write(entity, LocalTransform) {
      x += velocity.x * deltaTime
      y += velocity.y * deltaTime
    }
    val transform = entity[LocalTransform]
    val turnX = outward(transform.x, velocity.x, Playfield.HALF_WIDTH)
    val turnY = outward(transform.y, velocity.y, Playfield.HALF_HEIGHT)
    if (turnX || turnY) {
      replication.write(entity, Velocity) {
        if (turnX) x = -x
        if (turnY) y = -y
      }
    }
  }

  /** Whether a center at [position] moving at [speed] is past [half] and still heading out. */
  private fun outward(position: Float, speed: Float, half: Float): Boolean =
    (position > half && speed > 0f) || (position < -half && speed < 0f)
}
