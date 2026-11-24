package dev.chieppa.ease.net.input

/**
 * The host's store of one client's input, by tick. Frames arrive in packets that repeat the last
 * few ticks; the first copy of each tick is kept, and frames for ticks already taken are ignored.
 * Each tick the host [take]s the frame for that tick, or holds the last one when it never arrived.
 */
public class InputBuffer(layout: InputLayout, private val window: InputWindow) {
  private val capacity = window.ahead + InputPacket.MAX_FRAMES + 1
  private val frames = Array(capacity) { InputFrame(layout) }
  private val ticks = LongArray(capacity) { NONE }
  private val current = InputFrame(layout)
  private var nextTick = 0L

  /** The last tick whose own frame [take] returned, or -1 when none has arrived in time. */
  public var lastProcessedTick: Long = NONE
    private set

  /** How many packets were dropped for falling outside the window. */
  public var droppedPackets: Int = 0
    private set

  /**
   * The input for [tick]: its own frame when it arrived, else the last frame taken. Ticks taken
   * must rise.
   */
  public fun take(tick: Long): InputFrame {
    require(tick >= nextTick) { "Input for tick $tick was already taken" }
    val slot = slotOf(tick)
    if (ticks[slot] == tick) {
      current.copyFrom(frames[slot])
      lastProcessedTick = tick
    }
    nextTick = tick + 1
    return current
  }

  /**
   * Stores [packet]'s frames at the host's [hostTick], or drops the whole packet, and returns
   * false, when its newest tick lies outside the window.
   */
  internal fun offer(packet: InputPacket, hostTick: Long): Boolean {
    val newest = packet.newestTick
    if (newest > hostTick + window.ahead || newest < hostTick - window.behind) {
      droppedPackets++
      return false
    }
    for (index in 0 until packet.count) {
      val tick = newest - index
      if (tick < nextTick) break
      val slot = slotOf(tick)
      if (ticks[slot] != tick) {
        frames[slot].copyFrom(packet.frames[index])
        ticks[slot] = tick
      }
    }
    return true
  }

  private fun slotOf(tick: Long): Int = (tick % capacity).toInt()

  private companion object {
    const val NONE = -1L
  }
}
