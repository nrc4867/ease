package dev.chieppa.ease.physics.volume

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.FamilyOnAdd
import com.github.quillraven.fleks.FamilyOnRemove
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.physics.PhysicsWorld
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.jolt.BodyUserData

/**
 * Finds, each fixed step, which trigger volumes each `VolumeSensor` is inside, and reports to
 * [physics]'s events each entry and exit a trigger may report: one this world has authority over,
 * whose layers include the sensor's, and, for one that fires once, only its first entry. Nothing
 * here allocates once each sensor has been seen.
 */
public class TriggerSystem(private val physics: PhysicsWorld) :
  IteratingSystem(family { all(VolumeSensor, LocalTransform) }), FamilyOnAdd, FamilyOnRemove {
  private val replication: Replication = world.replication
  private val inside = SensorTable(PhysicsWorld.MAX_TRIGGER_HITS)
  private val found = IntArray(PhysicsWorld.MAX_TRIGGER_HITS)
  private val now = IntArray(PhysicsWorld.MAX_TRIGGER_HITS)

  override fun onAddEntity(entity: Entity) {
    val layer =
      (entity.getOrNull(CharacterController)?.layer ?: entity.getOrNull(Collider)?.layer).orEmpty()
    inside.add(entity.id, physics.layers.resolve(layer))
  }

  override fun onRemoveEntity(entity: Entity) {
    inside.remove(entity.id)
  }

  override fun onTickEntity(entity: Entity) {
    val transform = entity[LocalTransform]
    val sensed = entity[VolumeSensor]
    val bodies =
      physics.triggerBodiesAt(transform.x, transform.y + sensed.offsetY, transform.z, found)
    var count = 0
    for (index in 0 until bodies) {
      val trigger = BodyUserData.entityId(physics.bodyInterface.getUserData(found[index]))
      if (count < now.size && !contains(now, count, trigger)) now[count++] = trigger
    }
    for (index in 0 until inside.count(entity.id)) {
      val trigger = inside.at(entity.id, index)
      if (!contains(now, count, trigger)) exited(Entity(trigger), entity)
    }
    for (index in 0 until count) {
      if (!inside.has(entity.id, now[index])) entered(Entity(now[index]), entity)
    }
    inside.set(entity.id, now, count)
  }

  private fun entered(trigger: Entity, sensor: Entity) {
    if (!reports(trigger, sensor)) return
    val volume = trigger[TriggerVolume]
    if (volume.once) {
      if (volume.fired) return
      volume.fired = true
    }
    physics.events.triggerEntered(trigger, sensor)
  }

  private fun exited(trigger: Entity, sensor: Entity) {
    if (!reports(trigger, sensor) || trigger[TriggerVolume].once) return
    physics.events.triggerExited(trigger, sensor)
  }

  private fun reports(trigger: Entity, sensor: Entity): Boolean =
    trigger in world &&
      trigger.has(TriggerVolume) &&
      replication.hasAuthority(trigger) &&
      listens(trigger[TriggerVolume].layers, physics.layers.name(inside.layerOf(sensor.id)))

  /** Whether a trigger on [layers] reports a sensor on [sensorLayer]: any, when it names none. */
  private fun listens(layers: List<String>, sensorLayer: String): Boolean {
    var found = layers.isEmpty()
    for (index in layers.indices) found = found || layers[index] == sensorLayer
    return found
  }

  private fun contains(ids: IntArray, count: Int, id: Int): Boolean {
    for (index in 0 until count) if (ids[index] == id) return true
    return false
  }
}
