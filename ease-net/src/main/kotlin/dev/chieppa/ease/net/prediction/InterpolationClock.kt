package dev.chieppa.ease.net.prediction

import kotlin.math.abs

/**
 * The time, in host ticks, a client draws the entities it neither holds nor predicts at:
 * [delayTicks] behind the newest snapshot it applied, so there is usually a received state on
 * either side. It moves one tick per client tick, pulled gently toward that target so jitter in
 * arrivals never makes it jump, and starts again at the target only when it is far off.
 */
public class InterpolationClock(private val delayTicks: Int = DEFAULT_DELAY_TICKS) {
  init {
    require(delayTicks >= 0) { "A delay is 0 ticks or more, not $delayTicks" }
  }

  /** The time drawn now, or a negative number before the first snapshot. */
  public var time: Double = NOT_STARTED
    private set

  /** Moves on one client tick, the newest snapshot applied being of [latestSnapshotTick]. */
  public fun advance(latestSnapshotTick: Long) {
    if (latestSnapshotTick < 0) return
    val target = (latestSnapshotTick - delayTicks).toDouble()
    val next = time + 1.0
    val off = target - next
    time =
      if (time == NOT_STARTED || abs(off) > RESTART_TICKS) target
      else next + (off * PULL).coerceIn(-MAX_PULL, MAX_PULL)
  }

  public companion object {
    /** About 100 ms at 60 ticks a second. */
    public const val DEFAULT_DELAY_TICKS: Int = 6

    private const val NOT_STARTED = -1.0

    /** How much of its error the time makes up each tick. */
    private const val PULL = 0.1

    /** The most it moves faster or slower than one tick per tick. */
    private const val MAX_PULL = 0.25

    /** An error this large starts the time again at its target. */
    private const val RESTART_TICKS = 30.0
  }
}
