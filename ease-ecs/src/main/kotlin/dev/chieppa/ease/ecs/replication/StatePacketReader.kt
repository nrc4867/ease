package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity

/**
 * Applies received state packets to [replication]'s world, treating every byte as hostile. A packet
 * is read twice: once into scratch components to check every count, id, type and value, then, only
 * if all of it is acceptable, into the world. A refused packet changes nothing, and reading never
 * throws: it returns the [DecodeStatus] that refused it. Nothing here allocates.
 */
public class StatePacketReader(private val replication: Replication) {
  private val reader = BitReader()

  /** Reads the first [length] bytes of [bytes] and applies them, or refuses them all. */
  public fun read(bytes: ByteArray, length: Int): DecodeStatus =
    when {
      length < 0 || length > bytes.size -> DecodeStatus.TRUNCATED
      length > StatePackets.MAX_BYTES -> DecodeStatus.TOO_LONG
      else -> checkThenApply(bytes, length)
    }

  private fun checkThenApply(bytes: ByteArray, length: Int): DecodeStatus {
    reader.reset(bytes, length)
    pass(apply = false)
    if (!reader.ok) return reader.status
    reader.reset(bytes, length)
    pass(apply = true)
    return reader.status
  }

  private fun pass(apply: Boolean) {
    val count = reader.readBits(StatePackets.ENTITY_COUNT_BITS)
    if (count > StatePackets.MAX_ENTITIES) reader.fail(DecodeStatus.TOO_MANY_ENTITIES)
    var read = 0
    while (reader.ok && read < count) {
      entity(apply)
      read++
    }
    if (reader.ok && reader.remainingBits >= BYTE_BITS) reader.fail(DecodeStatus.TRAILING_DATA)
  }

  private fun entity(apply: Boolean) {
    val entity = entityFor(reader.readVarLong())
    val count = reader.readBits(StatePackets.COMPONENT_COUNT_BITS)
    if (count > replication.types.size) reader.fail(DecodeStatus.TOO_MANY_COMPONENTS)
    var read = 0
    while (reader.ok && read < count) {
      component(entity, apply)
      read++
    }
  }

  /** The replica [id] names, failing the read when it names none or one this world holds. */
  private fun entityFor(id: Long): Entity {
    val entity = if (id > 0L) replication.entities.entityOf(NetworkId(id)) else Entity.NONE
    when {
      !reader.ok -> Unit
      id <= 0L -> reader.fail(DecodeStatus.BAD_NETWORK_ID)
      entity == Entity.NONE -> reader.fail(DecodeStatus.UNKNOWN_ENTITY)
      replication.hasAuthority(entity) -> reader.fail(DecodeStatus.LOCAL_AUTHORITY)
    }
    return entity
  }

  private fun component(entity: Entity, apply: Boolean) {
    val typeId = reader.readBits(ReplicatedTypes.ID_BITS)
    val world = replication.world
    when {
      !reader.ok -> Unit
      typeId >= replication.types.size -> reader.fail(DecodeStatus.UNKNOWN_TYPE)
      !replication.types[typeId].isOn(world, entity) -> reader.fail(DecodeStatus.MISSING_COMPONENT)
      apply -> {
        replication.types[typeId].readInto(world, entity, reader)
        replication.received(entity, typeId)
      }
      else -> replication.types[typeId].check(reader)
    }
  }

  private companion object {
    const val BYTE_BITS = 8
  }
}
