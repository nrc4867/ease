package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform

/**
 * Moves every entity with a [Velocity] by it each tick, and turns it back along an axis once its
 * center passes the [Playfield] on that axis.
 */
public class BounceSystem : IteratingSystem(family { all(LocalTransform, Velocity) }) {
  override fun onTickEntity(entity: Entity) {
    val transform = entity[LocalTransform]
    val velocity = entity[Velocity]
    transform.x += velocity.x * deltaTime
    transform.y += velocity.y * deltaTime
    if (outward(transform.x, velocity.x, Playfield.HALF_WIDTH)) velocity.x = -velocity.x
    if (outward(transform.y, velocity.y, Playfield.HALF_HEIGHT)) velocity.y = -velocity.y
  }

  /** Whether a center at [position] moving at [speed] is past [half] and still heading out. */
  private fun outward(position: Float, speed: Float, half: Float): Boolean =
    (position > half && speed > 0f) || (position < -half && speed < 0f)
}
