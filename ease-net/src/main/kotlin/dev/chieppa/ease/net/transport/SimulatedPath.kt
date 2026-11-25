package dev.chieppa.ease.net.transport

import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.MessageQueue
import java.util.SplittableRandom

/**
 * One direction of a loopback link under its [conditions]: what one end sends and the other
 * receives. On a perfect network a message arrives as it is sent. Otherwise it waits in flight
 * until [advance] reaches its due tick, its fate drawn from [random] as it leaves: delay, jitter,
 * loss, duplication and resends as [LinkConditions] describes. Allocates nothing once grown.
 */
internal class SimulatedPath(private val random: SplittableRandom) {
  private val arrivals = Array(ChannelKind.entries.size) { MessageQueue(INITIAL_BYTES) }
  private val flight = InFlight()
  private var now = 0L
  private var budget = 0L
  private var lastOrderedDue = 0L

  /** The network this direction simulates; setting it fills the bandwidth budget again. */
  var conditions: LinkConditions = LinkConditions.PERFECT
    set(value) {
      field = value
      budget = value.bytesPerTick.toLong()
    }

  private val capped: Boolean
    get() = conditions.bytesPerTick != LinkConditions.UNCAPPED

  /** Sends [length] bytes of [bytes] from [offset] on the [kind] channel. */
  fun send(kind: ChannelKind, bytes: ByteArray, offset: Int, length: Int) {
    if (conditions.isPerfect && flight.waiting == 0 && lastOrderedDue <= now) {
      arrivals[kind.ordinal].push(bytes, offset, length)
      return
    }
    val slot = flight.add(kind, bytes, offset, length)
    if (flight.waiting == 1 && (!capped || budget > 0)) depart(slot)
  }

  /** Moves the next message that arrived on [kind] into [target]; see `Connection.receive`. */
  fun receive(kind: ChannelKind, target: ByteArray): Int = arrivals[kind.ordinal].pop(target)

  /** Moves on one tick: the bandwidth budget refills, waiting messages leave, due ones arrive. */
  fun advance() {
    now++
    if (capped) budget = minOf(budget + conditions.bytesPerTick, conditions.bytesPerTick.toLong())
    while (flight.waiting > 0 && (!capped || budget > 0)) depart(
      checkNotNull(flight.oldestWaiting())
    )
    while (true) arrive(flight.firstDue(now) ?: return)
  }

  /** Drops every message waiting, in flight or arrived. */
  fun clear() {
    arrivals.forEach { it.clear() }
    flight.clear()
  }

  /**
   * Sends the waiting message in [slot] on its way, drawing its fate: an unreliable one may be lost
   * or sent twice, a reliable one arrives after its resends, an ordered one never before the one
   * sent ahead of it.
   */
  private fun depart(slot: FlightSlot) {
    if (capped) budget -= slot.length
    when (slot.kind) {
      ChannelKind.UNRELIABLE ->
        if (chance(conditions.loss)) {
          flight.free(slot)
        } else {
          if (chance(conditions.duplication)) {
            schedule(flight.add(slot.kind, slot.bytes, 0, slot.length), delayedDue())
          }
          schedule(slot, delayedDue())
        }
      ChannelKind.RELIABLE_ORDERED -> {
        val due = maxOf(reliableDue(), lastOrderedDue)
        lastOrderedDue = due
        schedule(slot, due)
      }
      ChannelKind.RELIABLE_UNORDERED -> schedule(slot, reliableDue())
    }
  }

  /**
   * When a reliable message arrives: its delay plus a resend for each time the schedule loses it.
   */
  private fun reliableDue(): Long {
    var due = delayedDue()
    while (chance(conditions.loss)) due += conditions.resendTicks
    return due
  }

  private fun delayedDue(): Long {
    val jitter = conditions.jitterTicks
    val extra = if (jitter == 0) 0 else random.nextInt(jitter + 1)
    return now + conditions.latencyTicks + extra
  }

  private fun schedule(slot: FlightSlot, due: Long) {
    flight.schedule(slot, due)
    if (due <= now) arrive(slot)
  }

  private fun arrive(slot: FlightSlot) {
    arrivals[slot.kind.ordinal].push(slot.bytes, 0, slot.length)
    flight.free(slot)
  }

  private fun chance(odds: Double): Boolean = odds > 0.0 && random.nextDouble() < odds

  private companion object {
    const val INITIAL_BYTES = 16 * 1024
  }
}
