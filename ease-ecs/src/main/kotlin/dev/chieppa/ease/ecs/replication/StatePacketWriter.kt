package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity

/**
 * Writes state packets for [replication]'s world into a reused [BitWriter], allocating nothing:
 * [begin], then [entity] and its [component]s for each entity, then [end].
 */
public class StatePacketWriter(private val replication: Replication) {
  private var writer: BitWriter? = null
  private var entityCountAt = 0
  private var entityAt = 0
  private var entities = 0
  private var componentCountAt = 0
  private var components = 0
  private var current = Entity.NONE

  /** Starts a packet in [target], which it empties. */
  public fun begin(target: BitWriter) {
    target.reset()
    writer = target
    entityCountAt = target.bitPosition
    target.writeBits(0, StatePackets.ENTITY_COUNT_BITS)
    entities = 0
    current = Entity.NONE
  }

  /** Starts [entity]'s part of the packet. */
  public fun entity(entity: Entity) {
    val target = active()
    finishEntity(target)
    check(entities < StatePackets.MAX_ENTITIES) { "A packet carries ${entities} entities at most" }
    entityAt = target.bitPosition
    target.writeVarLong(replication.networkIdOf(entity).value)
    componentCountAt = target.bitPosition
    target.writeBits(0, StatePackets.COMPONENT_COUNT_BITS)
    components = 0
    entities++
    current = entity
  }

  /**
   * Writes the current entity's component of [type], with the change mask [fields], and returns
   * true. A component with a float the mask names that is not finite is left out, so replicas keep
   * its last good value and the rest of the packet still applies, and this returns false; a debug
   * build throws instead, naming the entity, component and field.
   */
  public fun component(type: ComponentType<*>, fields: Int): Boolean {
    val target = active()
    check(current != Entity.NONE) { "Start an entity before writing its components" }
    val id = replication.replicatedIdOf(type)
    val replicated = replication.types[id]
    val bad = replicated.nonFiniteField(replication.world, current, fields)
    if (bad >= 0) {
      refuseNonFinite(replicated.serializer, bad)
      return false
    }
    target.writeBits(id, ReplicatedTypes.ID_BITS)
    replicated.writeFrom(replication.world, current, target, fields)
    components++
    return true
  }

  /** How many entities the packet carries so far, the current one included. */
  public val entityCount: Int
    get() = entities

  /** The packet's length so far in bits, the current entity included. */
  public val bitLength: Int
    get() = active().bitPosition

  /**
   * Takes the current entity back out of the packet, as if [entity] had not been called for it,
   * such as when it would make the packet too long.
   */
  public fun dropEntity() {
    val target = active()
    check(current != Entity.NONE) { "There is no entity to drop" }
    target.rewind(entityAt)
    entities--
    current = Entity.NONE
  }

  private fun refuseNonFinite(serializer: ReplicatedSerializer<*>, field: Int) {
    check(!replication.debugChecks) {
      "${replication.describe(current)} holds a value that is not finite in " +
        "${serializer.name}.${serializer.fieldNames[field]}"
    }
  }

  /** Finishes the packet and returns its length in bytes. */
  public fun end(): Int {
    val target = active()
    finishEntity(target)
    target.writeBitsAt(entityCountAt, entities, StatePackets.ENTITY_COUNT_BITS)
    target.padToByte()
    writer = null
    check(target.byteLength <= StatePackets.MAX_BYTES) {
      "A packet is ${StatePackets.MAX_BYTES} bytes at most, not ${target.byteLength}"
    }
    return target.byteLength
  }

  private fun finishEntity(target: BitWriter) {
    if (current == Entity.NONE) return
    target.writeBitsAt(componentCountAt, components, StatePackets.COMPONENT_COUNT_BITS)
    current = Entity.NONE
  }

  private fun active(): BitWriter = checkNotNull(writer) { "Begin a packet first" }
}
