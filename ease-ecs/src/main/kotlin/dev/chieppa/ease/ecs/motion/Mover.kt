package dev.chieppa.ease.ecs.motion

import com.github.quillraven.fleks.Entity

/**
 * Moves a character: given the velocity its controls ask for, it changes the entity's
 * `LocalTransform` through `Replication.write`, so a predicted system can call it on a client as
 * the host's simulation does. [KinematicMover] moves freely; a physics character controller moves
 * the same way but against the world's colliders.
 */
public interface Mover {
  /**
   * Moves [entity] at ([velocityX], [velocityY], [velocityZ]) world units a second for [seconds]
   * and returns whether it was written; an entity the running code may not write stays put.
   */
  public fun move(
    entity: Entity,
    velocityX: Float,
    velocityY: Float,
    velocityZ: Float,
    seconds: Float,
  ): Boolean
}
