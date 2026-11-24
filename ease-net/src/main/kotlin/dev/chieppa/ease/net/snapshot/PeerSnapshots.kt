package dev.chieppa.ease.net.snapshot

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.ReplicatedFields
import dev.chieppa.ease.ecs.replication.ReplicatedTypes
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.StatePacketWriter
import dev.chieppa.ease.ecs.replication.StatePackets
import dev.chieppa.ease.ecs.replication.WireBits

private const val STATE_BITS = Snapshots.STATE_BYTES * WireBits.BYTE_BITS
private const val MAX_ID_BYTES = 10
private const val COUNT_BYTES = 2

/**
 * Builds the snapshots one peer receives: each carries, for every entity the host holds, the fields
 * the peer may not hold yet. Only components the peer knows are sent: those on the entities the
 * host held when the peer connected. When a snapshot is full, the next one starts after the last
 * entity it carried, so every entity gets its turn; an entity too large for any snapshot is left
 * out, and a debug build throws. Acknowledgements clear what the peer is known to hold. Nothing
 * here allocates once its tables have grown to the world's size.
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

  init {
    tracker.entities.forEach { entity -> if (replication.hasAuthority(entity)) know(entity) }
  }

  override fun changed(slot: Int, networkId: Long, fields: Int) {
    slots.fold(slot, networkId, fields)
  }

  /** Records that the peer holds [entity] with every replicated component it carries now. */
  private fun know(entity: Entity) {
    val networkId = replication.networkIdOf(entity).value
    for (typeId in 0 until tracker.typeCount) {
      if (!tracker.carries(entity, typeId)) continue
      val fields = replication.types[typeId].serializer.fieldCount
      slots.know(tracker.slotOf(entity, typeId), networkId, ReplicatedFields.allFields(fields))
    }
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
    val networkId = replication.networkIdOf(entity).value
    if (!replication.hasAuthority(entity) || !hasPending(entity, networkId)) return
    if (packets.entityCount >= StatePackets.MAX_ENTITIES) {
      full = true
      return
    }
    val mark = record.count
    packets.entity(entity)
    for (typeId in 0 until tracker.typeCount) {
      val slot = tracker.slotOf(entity, typeId)
      val fields = slots.pending(slot, networkId)
      if (fields == 0 || !tracker.carries(entity, typeId)) continue
      val type = replication.types[typeId].serializer.type
      if (packets.component(type, fields)) record.add(slot, networkId)
    }
    when {
      packets.bitLength > STATE_BITS -> overflow(mark, networkId)
      record.count == mark -> packets.dropEntity()
      else -> {
        for (index in mark until record.count) slots.carried(record.slotAt(index), tick)
        lastWritten = entity.id
      }
    }
  }

  /**
   * Takes back the entity [networkId] that made the snapshot too long. When it was alone, it can
   * never fit: it is left out, or a debug build throws; otherwise the snapshot is full.
   */
  private fun overflow(mark: Int, networkId: Long) {
    val bits = packets.bitLength
    packets.dropEntity()
    record.truncate(mark)
    if (packets.entityCount > 0) {
      full = true
      return
    }
    check(!replication.debugChecks) {
      "Entity #$networkId needs $bits bits, more than the $STATE_BITS a snapshot holds"
    }
  }

  private fun hasPending(entity: Entity, networkId: Long): Boolean {
    for (typeId in 0 until tracker.typeCount) {
      if (slots.pending(tracker.slotOf(entity, typeId), networkId) != 0) return true
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
      return MAX_ID_BYTES + COUNT_BYTES + WireBits.bytesFor(bits)
    }
  }
}
