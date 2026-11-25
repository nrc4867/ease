package dev.chieppa.ease.net.input

import dev.chieppa.ease.net.snapshot.Snapshots

/**
 * How early one client's input reaches the host, for the next snapshot to report: the least slack,
 * the newest tick an input packet carried less the host's tick it arrived at, among the packets
 * since the last report, and that packet's tick. A client's clock keeps the slack at its buffer.
 */
internal class InputSlack {
  /** The least slack since the last report. */
  var slack: Long = 0L
    private set

  /** The input tick the least slack was measured on, or -1 when no packet came since the report. */
  var inputTick: Long = NONE
    private set

  /** Takes a packet whose newest input is of [newestTick], arriving at the host's [hostTick]. */
  fun observe(newestTick: Long, hostTick: Long) {
    val measured = (newestTick - hostTick).coerceIn(-Snapshots.MAX_SLACK, Snapshots.MAX_SLACK)
    if (inputTick == NONE || measured < slack) {
      slack = measured
      inputTick = newestTick
    }
  }

  /** Starts over once a snapshot has reported the slack. */
  fun reported() {
    inputTick = NONE
  }

  private companion object {
    const val NONE = -1L
  }
}
