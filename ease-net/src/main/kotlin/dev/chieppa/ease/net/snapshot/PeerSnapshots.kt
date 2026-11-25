package dev.chieppa.ease.net.snapshot

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.ReplicatedFields
import dev.chieppa.ease.ecs.replication.ReplicatedTypes
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.StatePacketWriter
import dev.chieppa.ease.ecs.replication.StatePackets
import dev.chieppa.ease.ecs.replication.WireBits
import dev.chieppa.ease.net.input.InputSlack

private const val STATE_BITS = Snapshots.STATE_BYTES * WireBits.BYTE_BITS
private const val MAX_ID_BYTES = 10
private const val COUNT_BYTES = 2

/**
 * Builds the snapshots one peer receives: each carries, for the entities in the peer's view, the
 * fields the peer may not hold yet and may receive, owner-only fields only when the peer owns the
 * entity and all-but-owner fields only when it does not. Entities go in [PeerPriority] order until
 * the snapshot is full; an entity too large for any snapshot is left out, and a debug build throws.
 * Acknowledgements clear what the peer is known to hold. Nothing here allocates once its tables
 * have grown to the world's size.
 */
internal class PeerSnapshots(
  private val replication: Replication,
  private val tracker: ChangeTracker,
  private val priority: PeerPriority,
) : ChangeSink {
  private val slots = SlotStates()
  private val sent = Array(Snapshots.SENT_RING) { SentSnapshot() }
  private val header = BitWriter(Snapshots.MAX_HEADER_BYTES)
  private val packet = BitWriter(Snapshots.STATE_BYTES + maxEntityBytes(replication.types))
  private val packets = StatePacketWriter(replication)
  private val ownerMasks =
    IntArray(tracker.typeCount) { replication.types[it].serializer.ownerFields }
  private val othersMasks =
    IntArray(tracker.typeCount) { replication.types[it].serializer.othersFields }
  private var lastTick = Snapshots.NO_TICK

  // The snapshot being built: its record and whether it is full.
  private var record = sent[0]
  private var full = false

  override fun changed(slot: Int, networkId: Long, fields: Int) {
    slots.fold(slot, networkId, fields)
  }

  /**
   * Records that the peer holds [entity] under [networkId] with every replicated component it
   * carries now, as a spawn just sent them.
   */
  fun know(entity: Entity, networkId: Long) {
    for (typeId in 0 until tracker.typeCount) {
      if (tracker.carries(entity, typeId)) slots.know(tracker.slotOf(entity, typeId), networkId)
    }
    priority.reset(entity)
  }

  /** Records that the peer holds nothing of the entity [networkId] at entity id [entityId]. */
  fun forget(entityId: Int, networkId: Long) {
    for (typeId in 0 until tracker.typeCount) {
      slots.forget(entityId * tracker.typeCount + typeId, networkId)
    }
  }

  /**
   * Whether the peer holds exactly the replicated components [entity] carries under [networkId].
   */
  fun holdsAll(entity: Entity, networkId: Long): Boolean {
    var same = true
    for (typeId in 0 until tracker.typeCount) {
      val known = slots.knows(tracker.slotOf(entity, typeId), networkId)
      if (known != tracker.carries(entity, typeId)) same = false
    }
    return same
  }

  /**
   * Writes the snapshot of [tick] for the entities in [view] into [message], with [lastInputTick]
   * as the last input processed for this peer and its input's [slack], and returns its length.
   * Ticks must rise.
   */
  fun write(
    tick: Long,
    lastInputTick: Long,
    slack: InputSlack,
    view: ViewMembers,
    message: ByteArray,
  ): Int {
    require(tick > lastTick) { "Snapshot ticks rise: $tick follows $lastTick" }
    lastTick = tick
    record = sent[(tick % Snapshots.SENT_RING).toInt()]
    record.start(tick)
    full = false
    packets.begin(packet)
    var needs = 0L
    offer(view)
    for (index in 0 until priority.size) {
      val id = priority.idAt(index)
      val carried = !full && visit(Entity(id), view.networkIdOf(id), view.isOwner(id), tick)
      if (carried) needs = maxOf(needs, view.spawnOf(id))
      priority.settle(index, carried)
    }
    header.reset()
    header.writeVarLong(tick)
    header.writeVarLong(lastInputTick + 1)
    header.writeVarLong(needs)
    header.writeVarLong(slack.inputTick + 1)
    header.writeVarLong(Snapshots.zigzag(slack.slack))
    header.padToByte()
    slack.reported()
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

  /** Offers every entity in [view] with fields to send to [priority], then sorts them. */
  private fun offer(view: ViewMembers) {
    priority.clear()
    for (index in 0 until view.count) {
      val id = view.memberAt(index)
      val pending = hasPending(Entity(id), view.networkIdOf(id), view.isOwner(id))
      if (pending) priority.offer(Entity(id)) else priority.reset(Entity(id))
    }
    priority.sort()
  }

  /** Writes [entity]'s pending fields and returns whether the snapshot carries it. */
  private fun visit(entity: Entity, networkId: Long, owner: Boolean, tick: Long): Boolean {
    if (packets.entityCount >= StatePackets.MAX_ENTITIES) {
      full = true
      return false
    }
    val mark = record.count
    packets.entity(entity)
    for (typeId in 0 until tracker.typeCount) {
      val slot = tracker.slotOf(entity, typeId)
      val audience = if (owner) ownerMasks[typeId] else othersMasks[typeId]
      val fields = slots.pending(slot, networkId) and audience
      if (fields == 0 || !tracker.carries(entity, typeId)) continue
      val type = replication.types[typeId].serializer.type
      if (packets.component(type, fields)) record.add(slot, networkId)
    }
    val carried = packets.bitLength <= STATE_BITS && record.count > mark
    when {
      packets.bitLength > STATE_BITS -> overflow(mark, networkId)
      record.count == mark -> packets.dropEntity()
      else -> for (index in mark until record.count) slots.carried(record.slotAt(index), tick)
    }
    return carried
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

  private fun hasPending(entity: Entity, networkId: Long, owner: Boolean): Boolean {
    for (typeId in 0 until tracker.typeCount) {
      val pending = slots.pending(tracker.slotOf(entity, typeId), networkId)
      val audience = if (owner) ownerMasks[typeId] else othersMasks[typeId]
      if (pending and audience != 0) return true
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
