package dev.chieppa.ease.net.input

/**
 * A client's own input for each of its last [capacity] ticks, kept to replay them after a
 * correction. A tick it holds no frame for, such as one skipped while the clock caught up, replays
 * the frame before it, as the host holds a client's last frame for a tick that never arrived.
 * Frames are reused, never allocated per tick.
 */
public class InputRing(layout: InputLayout, private val capacity: Int) {
  init {
    require(capacity > 0) { "An input ring holds at least 1 tick, not $capacity" }
  }

  private val frames = Array(capacity) { InputFrame(layout) }
  private val ticks = LongArray(capacity) { NONE }
  private val none = InputFrame(layout)
  private var newestTick = NONE

  /** Keeps [frame] as the input of [tick]; ticks recorded must rise. */
  public fun record(tick: Long, frame: InputFrame) {
    require(tick > newestTick) { "Input ticks rise: $tick follows $newestTick" }
    val slot = slotOf(tick)
    frames[slot].copyFrom(frame)
    ticks[slot] = tick
    newestTick = tick
  }

  /**
   * Whether the input of [tick] can be replayed: it is one of the last [capacity] ticks recorded,
   * the newest included.
   */
  public fun reaches(tick: Long): Boolean =
    newestTick != NONE && tick <= newestTick && newestTick - tick < capacity

  /** The input of [tick]: its own frame, or the newest kept before it, or no input when none is. */
  public fun frameAt(tick: Long): InputFrame {
    var at = minOf(tick, newestTick)
    while (at >= 0 && reaches(at)) {
      val slot = slotOf(at)
      if (ticks[slot] == at) return frames[slot]
      at--
    }
    return none
  }

  private fun slotOf(tick: Long): Int = Math.floorMod(tick, capacity.toLong()).toInt()

  private companion object {
    const val NONE = -1L
  }
}
