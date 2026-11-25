package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity

/**
 * Applies received state packets to [replication]'s world, treating every byte as hostile. A packet
 * is read twice: once into scratch components to check every count, id, type and value, then, only
 * if all of it is acceptable, into the world. A refused packet changes nothing, and reading never
 * throws: it returns the [DecodeStatus] that refused it. State for an entity [skipped] names is
 * read and checked but applied to nothing. Nothing here allocates.
 */
public class StatePacketReader(
  private val replication: Replication,
  private val skipped: SkippedEntities = SkippedEntities.NONE,
) {
  private val reader = BitReader()
  private var skipping = false

  /** Reads the first [length] bytes of [bytes] and applies them, or refuses them all. */
  public fun read(bytes: ByteArray, length: Int): DecodeStatus = read(bytes, 0, length)

  /** Reads the [length] bytes of [bytes] from [offset] and applies them, or refuses them all. */
  public fun read(bytes: ByteArray, offset: Int, length: Int): DecodeStatus =
    when {
      offset < 0 || length < 0 || offset > bytes.size - length -> DecodeStatus.TRUNCATED
      length > StatePackets.MAX_BYTES -> DecodeStatus.TOO_LONG
      else -> checkThenApply(bytes, offset, length)
    }

  private fun checkThenApply(bytes: ByteArray, offset: Int, length: Int): DecodeStatus {
    reader.reset(bytes, offset, length)
    pass(apply = false)
    if (!reader.ok) return reader.status
    reader.reset(bytes, offset, length)
    replication.carried.startPacket()
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
    if (reader.ok && reader.remainingBits >= WireBits.BYTE_BITS)
      reader.fail(DecodeStatus.TRAILING_DATA)
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

  /**
   * The replica [id] names, failing the read when it names none or one this world holds, unless
   * [skipped] passes over it.
   */
  private fun entityFor(id: Long): Entity {
    val networkId = NetworkId(id)
    val entity = if (networkId.isValid) replication.entities.entityOf(networkId) else Entity.NONE
    skipping = reader.ok && networkId.isValid && skipped.skips(networkId, entity)
    when {
      !reader.ok || skipping -> Unit
      !networkId.isValid -> reader.fail(DecodeStatus.BAD_NETWORK_ID)
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
      skipping -> replication.types[typeId].check(reader)
      !replication.types[typeId].isOn(world, entity) -> reader.fail(DecodeStatus.MISSING_COMPONENT)
      apply -> {
        replication.types[typeId].readInto(world, entity, reader)
        replication.received(entity, typeId)
        replication.carried.mark(entity)
      }
      else -> replication.types[typeId].check(reader)
    }
  }
}
