package dev.chieppa.ease.ecs.replication

import dev.chieppa.ease.ecs.LocalTransform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** State packets from an authority world applied to a replica world. */
class StatePacketTest {
  private val pair = ReplicaPair(ENTITIES)

  private fun read(length: Int = pair.writer.byteLength): DecodeStatus =
    pair.reader.read(pair.writer.buffer, length)

  /** A packet whose one entity is [id] and whose one component has type id [typeId]. */
  private fun handMade(id: Long, typeId: Int = 0, componentCount: Int = 1): Int {
    val writer = pair.writer
    writer.reset()
    writer.writeBits(1, StatePackets.ENTITY_COUNT_BITS)
    writer.writeVarLong(id)
    writer.writeBits(componentCount, StatePackets.COMPONENT_COUNT_BITS)
    writer.writeBits(typeId, ReplicatedTypes.ID_BITS)
    writer.writeBits(0, LanternReplicated.fieldCount)
    writer.padToByte()
    return writer.byteLength
  }

  @Test
  fun `a packet carries the authority's state to the replica`() {
    pair.held.forEachIndexed { index, entity ->
      pair.authority.replication.write(entity, Lantern) {
        fuel = index * 2f
        charges = index
        lit = index % 2 == 0
      }
    }

    assertEquals(DecodeStatus.OK, read(pair.writeAll()))

    pair.mirrored.forEachIndexed { index, entity ->
      val lantern = ReplicationWorlds.lanternOf(pair.replica, entity)
      assertEquals(index * 2f, lantern.fuel)
      assertEquals(index, lantern.charges)
      assertEquals(index % 2 == 0, lantern.lit)
    }
  }

  @Test
  fun `received state passes the replica's end-of-tick check`() {
    pair.authority.replication.write(pair.held[0], Lantern) { fuel = 3f }
    read(pair.writeAll())

    pair.replica.update(DELTA)
  }

  @Test
  fun `a packet may carry several component types for one entity`() {
    val entity = pair.held[0]
    pair.authority.replication.write(entity, LocalTransform) { x = 6f }
    pair.packets.begin(pair.writer)
    pair.packets.entity(entity)
    pair.packets.component(Lantern, ReplicaPair.ALL_FIELDS)
    pair.packets.component(LocalTransform, 1)

    assertEquals(DecodeStatus.OK, read(pair.packets.end()))
    assertEquals(6f, with(pair.replica) { pair.mirrored[0][LocalTransform].x })
  }

  @Test
  fun `state for an unknown entity, type or missing component is refused`() {
    val known = pair.authority.networkIdOf(pair.held[0]).value
    val bare = pair.replica.entity { it += Authority(PeerId.HOST, PeerId.HOST) }
    val bareId = pair.replica.networkIdOf(bare).value

    assertEquals(DecodeStatus.UNKNOWN_ENTITY, read(handMade(id = 999)))
    assertEquals(DecodeStatus.BAD_NETWORK_ID, read(handMade(id = 0)))
    assertEquals(DecodeStatus.UNKNOWN_TYPE, read(handMade(known, typeId = 7)))
    assertEquals(DecodeStatus.TOO_MANY_COMPONENTS, read(handMade(known, componentCount = 3)))
    assertEquals(DecodeStatus.MISSING_COMPONENT, read(handMade(bareId)))
  }

  @Test
  fun `state for an entity this world holds is refused`() {
    val own = ReplicationWorlds.lanternEntity(pair.replica)

    val status = read(handMade(pair.replica.networkIdOf(own).value))

    assertEquals(DecodeStatus.LOCAL_AUTHORITY, status)
  }

  @Test
  fun `trailing bytes and oversized packets are refused`() {
    val length = pair.writeAll()

    assertEquals(DecodeStatus.TRAILING_DATA, read(length + 1))
    assertEquals(DecodeStatus.TOO_LONG, pair.reader.read(ByteArray(OVERSIZED), OVERSIZED))
  }

  @Test
  fun `a refused packet changes nothing, not even the entities before the problem`() {
    pair.authority.replication.write(pair.held[0], Lantern) { fuel = 4f }
    val length = pair.writeAll()
    val before = pair.replicaState()

    val status = read(length - 1)

    assertEquals(DecodeStatus.TRUNCATED, status)
    assertEquals(before, pair.replicaState())
    assertTrue(before.isNotEmpty())
  }

  private companion object {
    const val ENTITIES = 3
    const val OVERSIZED = 2_000
    const val DELTA = 1f / 60f
  }
}
