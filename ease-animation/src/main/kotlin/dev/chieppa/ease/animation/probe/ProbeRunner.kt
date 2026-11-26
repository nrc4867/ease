package dev.chieppa.ease.animation.probe

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.events.AnimationEvents
import dev.chieppa.ease.animation.runtime.AnimatedPoses
import dev.chieppa.ease.animation.runtime.AnimatorInstance
import dev.chieppa.ease.physics.PhysicsStepSystem
import dev.chieppa.ease.physics.PhysicsWorld
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * Runs an animator's probes on its clip events: each probe whose event fired finds where its socket
 * or bone is in the world this tick, asks the world's physics for the surface below it (a downward
 * ray from `lift` above the point to `depth` below it) and reports the result to [events]. A world
 * without physics reports -1. Running allocates nothing.
 */
internal class ProbeRunner(private val world: World, private val events: AnimationEvents) {
  private val point = FloatArray(WORLD_TRANSFORM_FLOATS)
  private var physics: PhysicsWorld? = null
  private var looked = false

  /** Runs every probe of [instance] listening for [event]. */
  fun run(
    instance: AnimatorInstance,
    event: String,
    poses: AnimatedPoses,
    worlds: WorldTransforms?,
  ) {
    val probes = instance.graph.asset.probes
    for (index in 0 until probes.size) {
      val probe = probes[index]
      if (probe.event != event || !place(instance, index, poses, worlds)) continue
      val x = point[0]
      val y = point[1]
      val z = point[2]
      val surface =
        physicsWorld()?.surfaceBelow(x, y + probe.lift, z, probe.lift + probe.depth)
          ?: PhysicsWorld.NO_SURFACE
      events.probe(instance.entity, probe.name, surface, x, y, z)
    }
  }

  /** Writes probe [index]'s world transform into [point]; false when its bone is not bound. */
  private fun place(
    instance: AnimatorInstance,
    index: Int,
    poses: AnimatedPoses,
    worlds: WorldTransforms?,
  ): Boolean {
    val socket = instance.probeSockets[index]
    val bone = instance.probeBones[index]
    return when {
      socket >= 0 -> poses.socketWorld(instance, socket, point)
      bone == Entity.NONE -> false
      else -> {
        if (worlds != null) worlds.write(bone, point, 0)
        else WorldTransforms.writeLocal(world, bone, point, 0)
        true
      }
    }
  }

  private fun physicsWorld(): PhysicsWorld? {
    if (!looked) {
      looked = true
      physics = world.systemOrNull<PhysicsStepSystem>()?.physics
    }
    return physics
  }
}
