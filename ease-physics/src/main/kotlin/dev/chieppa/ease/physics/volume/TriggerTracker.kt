package dev.chieppa.ease.physics.volume

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.physics.PhysicsWorld
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.character.CharacterController

/**
 * Which trigger volumes each sensor is inside, and the entries and exits that reports to
 * [physics]'s events. A trigger reports an entry only from the world with authority over it, only
 * for sensors on its layers, and, when it fires once, only its first. Every reported entry of a
 * trigger that does not fire once is paired with one exit: when the sensor leaves it, when either
 * leaves the world, or when the trigger stops being one.
 */
internal class TriggerTracker(private val physics: PhysicsWorld) {
  /** For each sensor, the triggers whose entry it reported and whose exit it owes. */
  private val inside = SensorTable(PhysicsWorld.MAX_TRIGGER_HITS)
  private val kept = IntArray(PhysicsWorld.MAX_TRIGGER_HITS)

  /** Notes that [sensor] is inside the [count] triggers in [now], reporting what changed. */
  fun update(world: World, sensor: Entity, now: IntArray, count: Int) {
    for (index in 0 until inside.count(sensor.id)) {
      val trigger = inside.at(sensor.id, index)
      if (!contains(now, count, trigger)) physics.events.triggerExited(Entity(trigger), sensor)
    }
    var owed = 0
    for (index in 0 until count) {
      val trigger = now[index]
      if (inside.has(sensor.id, trigger) || entered(world, Entity(trigger), sensor)) {
        kept[owed++] = trigger
      }
    }
    inside.set(sensor.id, kept, owed)
  }

  /** [sensor] is leaving the world or stops sensing: it exits every trigger it is inside. */
  fun sensorRemoved(sensor: Entity) {
    for (index in 0 until inside.count(sensor.id)) {
      physics.events.triggerExited(Entity(inside.at(sensor.id, index)), sensor)
    }
    inside.clear(sensor.id)
  }

  /** [trigger] is leaving the world or stops being a trigger: every sensor inside exits it. */
  fun triggerRemoved(trigger: Entity) {
    for (id in 0 until inside.size) {
      if (inside.has(id, trigger.id)) {
        inside.drop(id, trigger.id)
        physics.events.triggerExited(trigger, Entity(id))
      }
    }
  }

  /** Reports [sensor] entering [trigger] if the trigger reports it; true when an exit is owed. */
  private fun entered(world: World, trigger: Entity, sensor: Entity): Boolean {
    val volume =
      if (reports(world, trigger, sensor)) with(world) { trigger[TriggerVolume] } else null
    if (volume == null || (volume.once && volume.fired)) return false
    volume.fired = volume.once
    physics.events.triggerEntered(trigger, sensor)
    return !volume.once
  }

  private fun reports(world: World, trigger: Entity, sensor: Entity): Boolean =
    with(world) {
      trigger.has(TriggerVolume) &&
        world.replication.hasAuthority(trigger) &&
        listens(trigger[TriggerVolume].layers, layerOf(world, sensor))
    }

  /** The name of [sensor]'s collision layer: its controller's or collider's, else the first. */
  private fun layerOf(world: World, sensor: Entity): String =
    with(world) {
      val named = sensor.getOrNull(CharacterController)?.layer ?: sensor.getOrNull(Collider)?.layer
      physics.layers.name(physics.layers.resolve(named.orEmpty()))
    }

  /** Whether a trigger on [layers] reports a sensor on [sensorLayer]: any, when it names none. */
  private fun listens(layers: List<String>, sensorLayer: String): Boolean {
    var found = layers.isEmpty()
    for (index in layers.indices) found = found || layers[index] == sensorLayer
    return found
  }

  private fun contains(ids: IntArray, count: Int, id: Int): Boolean {
    var found = false
    for (index in 0 until count) found = found || ids[index] == id
    return found
  }
}
