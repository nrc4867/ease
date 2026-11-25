package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.motion.KinematicMover
import dev.chieppa.ease.ecs.motion.Mover
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.mayWrite
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.input.PeerInputs
import dev.chieppa.ease.net.input.peerInputs

/**
 * Moves each courier from its owner's input, a predicted system: the stick's x and y, from -1 to 1,
 * become its [Velocity] at up to its [Courier.pace], and a [Mover] carries it there. The host runs
 * it for every peer's courier; each client runs it ahead of the host for its own.
 */
public class CourierSystem :
  IteratingSystem(family { all(Courier, LocalTransform, Velocity, Authority) }) {
  private val replication: Replication = world.replication
  private val inputs: PeerInputs = world.peerInputs
  private val mover: Mover = KinematicMover(world)

  override fun onTickEntity(entity: Entity) {
    if (!replication.mayWrite(entity)) return
    val input = inputs.of(entity[Authority].owner) ?: return
    val pace = entity[Courier].pace
    val speedX = input.axis(AXIS_X) * pace
    val speedY = input.axis(AXIS_Y) * pace
    replication.write(entity, Velocity) {
      x = speedX
      y = speedY
    }
    mover.move(entity, speedX, speedY, 0f, deltaTime)
  }

  public companion object {
    /** The input axis steering along x. */
    public const val AXIS_X: Int = 0

    /** The input axis steering along y. */
    public const val AXIS_Y: Int = 1
  }
}
