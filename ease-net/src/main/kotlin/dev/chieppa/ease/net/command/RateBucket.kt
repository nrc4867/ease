package dev.chieppa.ease.net.command

/**
 * A token bucket for one [RateLimit], counted in ticks with whole numbers: one command costs
 * `ticks` units, each tick earns `commands` units, and the bucket holds `burst` commands. It
 * refills when used, so nothing runs per tick. It starts full.
 */
internal class RateBucket(private val limit: RateLimit) {
  private val capacity = limit.burst.toLong() * limit.ticks
  private var level = capacity
  private var lastTick = Long.MIN_VALUE

  /** Takes one command's worth at [tick] and returns true, or returns false when there is none. */
  fun take(tick: Long): Boolean {
    if (lastTick != Long.MIN_VALUE && tick > lastTick) {
      val earned = (tick - lastTick).coerceAtMost(capacity) * limit.commands
      level = minOf(capacity, level + earned)
    }
    if (tick > lastTick) lastTick = tick
    if (level < limit.ticks) return false
    level -= limit.ticks
    return true
  }
}
