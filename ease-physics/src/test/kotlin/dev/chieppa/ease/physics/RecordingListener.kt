package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.physics.events.PhysicsListener

/** Writes down every physics event, as a script subscribing to them would receive them. */
class RecordingListener : PhysicsListener {
  val entered = mutableListOf<Pair<Entity, Entity>>()
  val exited = mutableListOf<Pair<Entity, Entity>>()
  val footsteps = mutableListOf<Int>()
  val groundChanges = mutableListOf<Int>()

  override fun triggerEntered(trigger: Entity, sensor: Entity) {
    entered += trigger to sensor
  }

  override fun triggerExited(trigger: Entity, sensor: Entity) {
    exited += trigger to sensor
  }

  override fun footstep(character: Entity, surface: Int) {
    footsteps += surface
  }

  override fun groundSurfaceChanged(character: Entity, surface: Int) {
    groundChanges += surface
  }
}
