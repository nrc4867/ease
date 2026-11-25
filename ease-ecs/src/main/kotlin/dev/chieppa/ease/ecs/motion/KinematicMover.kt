package dev.chieppa.ease.ecs.motion

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.mayWrite
import dev.chieppa.ease.ecs.replication.replication

/**
 * The [Mover] with nothing in its way: it adds velocity times time to the entity's position. Make
 * it where [world] is current, as inside a system, or pass the world. Nothing here allocates.
 */
public class KinematicMover(world: World) : Mover {
  private val replication: Replication = world.replication

  override fun move(
    entity: Entity,
    velocityX: Float,
    velocityY: Float,
    velocityZ: Float,
    seconds: Float,
  ): Boolean {
    if (!replication.mayWrite(entity)) return false
    return replication.write(entity, LocalTransform) {
      x += velocityX * seconds
      y += velocityY * seconds
      z += velocityZ * seconds
    }
  }
}
