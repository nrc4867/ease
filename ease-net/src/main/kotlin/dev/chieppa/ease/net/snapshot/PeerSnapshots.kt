package dev.chieppa.ease.net.snapshot

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.ReplicatedFields
import dev.chieppa.ease.ecs.replication.ReplicatedTypes
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.StatePacketWriter
import dev.chieppa.ease.ecs.replication.StatePackets

private const val BYTE_BITS = 8
private const val MAX_ID_BYTES = 10
private const val COUNT_BYTES = 2

/**
 * Builds the snapshots one peer receives: each carries, for every entity the host holds, the fields
 * the peer may not hold yet. When a snapshot is full, the next one starts after the last entity it
 * carried, so every entity gets its turn. Acknowledgements clear what the peer is known to hold.
 * Nothing here allocates once its tables have grown to the world's size.
 */
internal class PeerSnapshots(
  private val replication: Replication,
  private val tracker: ChangeTracker,
) : ChangeSink {
  private val slots = SlotStates()
  private val sent = Array(Snapshots.SENT_RING) { SentSnapshot() }
  private val header = BitWriter(Snapshots.MAX_HEADER_BYTES)
  private val packet = BitWriter(Snapshots.STATE_BYTES + maxEntityBytes(replication.types))
  private val packets = StatePacketWriter(replication)
  private var resumeAfter = -1
  private var lastTick = Snapshots.NO_TICK

  // The snapshot being built: its record, whether it is full, and its last entity.
  private var record = sent[0]
  private var full = false
  private var lastWritten = -1

  override fun changed(slot: Int, networkId: Long, fields: Int, allFields: Int) {
    slots.fold(slot, networkId, fields, allFields)
  }

  /**
   * Writes the snapshot of [tick] into [message], with [lastInputTick] as the last input processed
   * for this peer, and returns its length. Ticks must rise from one snapshot to the next.
   */
  fun write(tick: Long, lastInputTick: Long, message: ByteArray): Int {
    require(tick > lastTick) { "Snapshot ticks rise: $tick follows $lastTick" }
    lastTick = tick
    header.reset()
    header.writeVarLong(tick)
    header.writeVarLong(lastInputTick + 1)
    header.padToByte()
    record = sent[(tick % Snapshots.SENT_RING).toInt()]
    record.start(tick)
    full = false
    lastWritten = -1
    packets.begin(packet)
    val after = resumeAfter
    tracker.entities.forEach { if (!full && it.id > after) visit(it, tick) }
    tracker.entities.forEach { if (!full && it.id <= after) visit(it, tick) }
    if (full) resumeAfter = lastWritten
    val stateBytes = packets.end()
    System.arraycopy(header.buffer, 0, message, 0, header.byteLength)
    System.arraycopy(packet.buffer, 0, message, header.byteLength, stateBytes)
    return header.byteLength + stateBytes
  }

  /**
   * The peer applied the snapshot of [latest] and, for each set bit `i` of [earlier], the one of
   * `latest - 1 - i`. Ticks no snapshot was sent on, or too old to be remembered, are ignored.
   */
  fun acknowledge(latest: Long, earlier: Int) {
    acknowledgeOne(latest)
    for (bit in 0 until Int.SIZE_BITS) {
      if ((earlier ushr bit) and 1 != 0) acknowledgeOne(latest - 1 - bit)
    }
  }

  private fun acknowledgeOne(tick: Long) {
    if (tick < 0) return
    val snapshot = sent[(tick % Snapshots.SENT_RING).toInt()]
    if (snapshot.tick != tick || snapshot.acknowledged) return
    snapshot.acknowledged = true
    for (index in 0 until snapshot.count) {
      slots.confirm(snapshot.slotAt(index), snapshot.networkIdAt(index), tick)
    }
  }

  private fun visit(entity: Entity, tick: Long) {
    if (!replication.hasAuthority(entity) || !hasPending(entity)) return
    if (packets.entityCount >= StatePackets.MAX_ENTITIES) {
      full = true
      return
    }
    val mark = record.count
    val networkId = replication.networkIdOf(entity).value
    packets.entity(entity)
    for (typeId in 0 until tracker.typeCount) {
      val slot = tracker.slotOf(entity, typeId)
      val fields = slots.pending(slot)
      if (fields == 0 || !tracker.carries(entity, typeId)) continue
      val type = replication.types[typeId].serializer.type
      if (packets.component(type, fields)) record.add(slot, networkId)
    }
    when {
      packets.bitLength > Snapshots.STATE_BYTES * BYTE_BITS -> {
        packets.dropEntity()
        record.truncate(mark)
        full = true
      }
      record.count == mark -> packets.dropEntity()
      else -> {
        for (index in mark until record.count) slots.carried(record.slotAt(index), tick)
        lastWritten = entity.id
      }
    }
  }

  private fun hasPending(entity: Entity): Boolean {
    for (typeId in 0 until tracker.typeCount) {
      if (slots.pending(tracker.slotOf(entity, typeId)) != 0) return true
    }
    return false
  }

  private companion object {
    /** The most bytes one entity can take in a state packet: its id, counts and every field. */
    fun maxEntityBytes(types: ReplicatedTypes): Int {
      var bits = 0
      for (id in 0 until types.size) {
        val fields = types[id].serializer.fieldCount
        bits += ReplicatedTypes.ID_BITS + ReplicatedFields.MAX_FIELDS * (fields + 1)
      }
      return MAX_ID_BYTES + COUNT_BYTES + bits / BYTE_BITS + 1
    }
  }
}
