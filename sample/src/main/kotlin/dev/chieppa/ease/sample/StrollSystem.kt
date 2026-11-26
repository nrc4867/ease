package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.replication
import kotlin.math.abs
import kotlin.math.sqrt

/** The y and w of a quarter turn about y, so a stroller faces along x. */
private const val QUARTER = 0.70710677f

/**
 * Walks every [Stroller] on the host: from a stop it speeds up to its speed, slows so it stops at
 * the end of its reach, turns to face the other way, waits, and walks back. Only the transform
 * replicates; every peer's animator works out the walk from how fast the node moves.
 */
public class StrollSystem : IteratingSystem(family { all(Stroller, LocalTransform) }) {
  private val replication: Replication = world.replication

  override fun onTickEntity(entity: Entity) {
    if (!replication.hasAuthority(entity)) return
    val stroll = entity[Stroller]
    if (stroll.waiting > 0f) {
      stroll.waiting -= deltaTime
      return
    }
    val x = entity[LocalTransform].x
    val remaining = abs(stroll.direction * stroll.reach - x)
    stroll.walked += deltaTime
    val speed =
      minOf(
        stroll.speed,
        stroll.acceleration * stroll.walked,
        sqrt(2f * stroll.acceleration * remaining),
      )
    val step = minOf(speed * deltaTime, remaining)
    replication.write(entity, LocalTransform) { this.x += stroll.direction * step }
    if (remaining - step <= ARRIVED) {
      stroll.waiting = stroll.pause
      turn(entity, stroll)
    }
  }

  private fun turn(entity: Entity, stroll: Stroller) {
    stroll.direction = -stroll.direction
    stroll.walked = 0f
    replication.write(entity, LocalTransform) {
      rotationX = 0f
      rotationY = QUARTER * stroll.direction
      rotationZ = 0f
      rotationW = QUARTER
    }
  }

  private companion object {
    /** Closer than this to its end, in world units, a stroller has arrived. */
    const val ARRIVED = 1e-4f
  }
}
