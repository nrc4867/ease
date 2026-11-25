package dev.chieppa.ease.net.snapshot

import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.DecodeStatus
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.StatePacketReader
import dev.chieppa.ease.ecs.replication.WireBits

/**
 * Applies the snapshots a client receives, treating every byte as hostile. Only a snapshot newer
 * than every one applied so far applies; an older or repeated one is skipped, since the host's
 * deltas assume the client holds what the newest applied snapshot carried. So is one that needs a
 * spawn the client has not applied yet; a later snapshot carries its fields. Otherwise it applies,
 * and [history] passes over the entities spawned again or despawned since it was sent. It keeps
 * what the client acknowledges: the newest applied tick and which of the 32 before it were applied
 * too.
 */
internal class SnapshotReceiver(replication: Replication, private val history: ViewHistory) {
  private val header = BitReader()
  private val state = StatePacketReader(replication, history)

  // The header of the snapshot being read.
  private var tick = 0L
  private var inputPlusOne = 0L
  private var needs = 0L

  /** The tick of the newest applied snapshot, or [Snapshots.NO_TICK]. */
  var latestTick: Long = Snapshots.NO_TICK
    private set

  /** Bit `i` set: the snapshot of `latestTick - 1 - i` was applied too. */
  var earlierApplied: Int = 0
    private set

  /** The last input tick the host had processed for this client, or [Snapshots.NO_TICK]. */
  var lastInputTick: Long = Snapshots.NO_TICK
    private set

  /** Reads the first [length] bytes of [bytes]: applies them, skips them, or refuses. */
  fun receive(bytes: ByteArray, length: Int): DecodeStatus {
    if (length < 0 || length > bytes.size) return DecodeStatus.TRUNCATED
    val problem = readHeader(bytes, length)
    val skip = tick <= latestTick || needs > history.applied
    return if (problem != DecodeStatus.OK || skip) problem else applyState(bytes, length)
  }

  private fun readHeader(bytes: ByteArray, length: Int): DecodeStatus {
    header.reset(bytes, length)
    tick = header.readVarLong()
    inputPlusOne = header.readVarLong()
    needs = header.readVarLong()
    return when {
      !header.ok -> header.status
      tick < 0 || inputPlusOne < 0 || needs < 0 -> DecodeStatus.MALFORMED
      inputPlusOne - 1 > tick -> DecodeStatus.BAD_VALUE
      else -> DecodeStatus.OK
    }
  }

  private fun applyState(bytes: ByteArray, length: Int): DecodeStatus {
    val headerBytes = WireBits.bytesFor(header.bitPosition)
    history.reading(needs)
    val status = state.read(bytes, headerBytes, length - headerBytes)
    if (status == DecodeStatus.OK) {
      applied(tick)
      lastInputTick = inputPlusOne - 1
      history.appliedThrough(needs)
    }
    return status
  }

  private fun applied(tick: Long) {
    val shift = tick - latestTick
    earlierApplied =
      when {
        latestTick == Snapshots.NO_TICK || shift > Int.SIZE_BITS -> 0
        shift == Int.SIZE_BITS.toLong() -> 1 shl (Int.SIZE_BITS - 1)
        else -> (earlierApplied shl shift.toInt()) or (1 shl (shift.toInt() - 1))
      }
    latestTick = tick
  }
}
