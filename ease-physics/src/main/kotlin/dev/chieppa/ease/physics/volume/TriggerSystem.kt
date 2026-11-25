package dev.chieppa.ease.physics.volume

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.FamilyOnRemove
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.physics.PhysicsWorld
import dev.chieppa.ease.physics.jolt.BodyUserData

/**
 * Finds, each fixed step, which trigger volumes each `VolumeSensor` is inside, and hands it to
 * [physics]'s trigger tracker, which reports entries and exits; a sensor leaving the world exits
 * every trigger it was in. Nothing here allocates once each sensor has been seen.
 */
public class TriggerSystem(private val physics: PhysicsWorld) :
  IteratingSystem(family { all(VolumeSensor, LocalTransform) }), FamilyOnRemove {
  private val found = IntArray(PhysicsWorld.MAX_TRIGGER_HITS)
  private val now = IntArray(PhysicsWorld.MAX_TRIGGER_HITS)

  override fun onRemoveEntity(entity: Entity) {
    physics.triggers.sensorRemoved(entity)
  }

  override fun onTickEntity(entity: Entity) {
    val transform = entity[LocalTransform]
    val sensed = entity[VolumeSensor]
    val bodies =
      physics.triggerBodiesAt(transform.x, transform.y + sensed.offsetY, transform.z, found)
    var count = 0
    for (index in 0 until bodies) {
      val trigger = BodyUserData.entityId(physics.bodyInterface.getUserData(found[index]))
      if (count < now.size && !seen(count, trigger)) now[count++] = trigger
    }
    physics.triggers.update(world, entity, now, count)
  }

  private fun seen(count: Int, id: Int): Boolean {
    var found = false
    for (index in 0 until count) found = found || now[index] == id
    return found
  }
}
