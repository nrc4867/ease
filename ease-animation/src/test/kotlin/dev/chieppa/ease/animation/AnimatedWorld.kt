package dev.chieppa.ease.animation

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.assets.ParameterDrive
import dev.chieppa.ease.animation.runtime.AnimatorSystem
import dev.chieppa.ease.ecs.LocalTransformReplicated
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.physics.Physics
import dev.chieppa.ease.physics.setup.PhysicsSettings
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * A headless world, run as [peer], that plays [TestRig]'s animator: scene nodes, physics when
 * [physics] is given, what [extra] adds, the animator system and world transforms. Local transforms
 * replicate with the debug checks on, so a pose written to a joint's `LocalTransform` would throw.
 */
class AnimatedWorld(
  drive: ParameterDrive = ParameterDrive.GAME,
  physics: PhysicsSettings? = null,
  peer: PeerId = PeerId.HOST,
  extra: PhasedSystems.() -> Unit = {},
) : AutoCloseable {
  val world: World =
    phasedWorld(
      replication = ReplicationSettings(peer, listOf(LocalTransformReplicated), debugChecks = true)
    ) {
      add(Phase.INPUT, SceneNodes())
      physics?.let { Physics.addTo(this, it) }
      extra()
      Animation.addTo(this, TestRig.assets(drive))
      add(Phase.EXTRACT, WorldTransforms())
    }

  val animators: AnimatorSystem = world.animators
  private val transforms = world.system<WorldTransforms>()
  private val out = FloatArray(WORLD_TRANSFORM_FLOATS)

  /** Runs [ticks] fixed steps of [STEP] seconds. */
  fun step(ticks: Int = 1) = repeat(ticks) { world.update(STEP) }

  /** [entity]'s world transform as the last tick left it. */
  fun worldOf(entity: Entity): FloatArray {
    transforms.write(entity, out, 0)
    return out.copyOf()
  }

  override fun close() = world.dispose()

  companion object {
    const val STEP = 1f / 60f
  }
}
