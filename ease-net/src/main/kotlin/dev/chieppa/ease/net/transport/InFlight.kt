package dev.chieppa.ease.net.transport

import dev.chieppa.ease.net.channel.ChannelKind

/**
 * The messages one direction of a simulated link holds, each in a [FlightSlot]: waiting for
 * bandwidth, or sent and due at a tick. Slots and their buffers are reused, so once grown it
 * allocates nothing.
 */
internal class InFlight {
  private var slots = Array(INITIAL_SLOTS) { FlightSlot() }
  private var nextOrder = 0L

  /** How many messages wait for bandwidth. */
  var waiting: Int = 0
    private set

  /** Holds a copy of [length] bytes of [bytes] from [offset], waiting, and returns its slot. */
  fun add(kind: ChannelKind, bytes: ByteArray, offset: Int, length: Int): FlightSlot {
    val slot = freeSlot()
    if (slot.bytes.size < length) slot.bytes = ByteArray(length)
    System.arraycopy(bytes, offset, slot.bytes, 0, length)
    slot.kind = kind
    slot.length = length
    slot.due = FlightSlot.WAITING
    slot.order = nextOrder++
    slot.used = true
    waiting++
    return slot
  }

  /** Marks the message in [slot] sent, due at [tick]. */
  fun schedule(slot: FlightSlot, tick: Long) {
    if (slot.isWaiting) waiting--
    slot.due = tick
  }

  /** Empties [slot]. */
  fun free(slot: FlightSlot) {
    if (slot.isWaiting) waiting--
    slot.used = false
  }

  /** The waiting message sent first, or null. */
  fun oldestWaiting(): FlightSlot? {
    var best: FlightSlot? = null
    for (slot in slots) {
      if (slot.isWaiting && (best == null || slot.order < best.order)) best = slot
    }
    return best
  }

  /** The sent message due at or before [tick] that arrives first, or null. */
  fun firstDue(tick: Long): FlightSlot? {
    var best: FlightSlot? = null
    for (slot in slots) {
      if (slot.isDueBy(tick) && slot.arrivesBefore(best)) best = slot
    }
    return best
  }

  /** Empties every slot. */
  fun clear() {
    for (slot in slots) slot.used = false
    waiting = 0
  }

  private fun freeSlot(): FlightSlot {
    for (slot in slots) if (!slot.used) return slot
    val old = slots
    slots = Array(old.size * 2) { if (it < old.size) old[it] else FlightSlot() }
    return slots[old.size]
  }

  private companion object {
    const val INITIAL_SLOTS = 16
  }
}
