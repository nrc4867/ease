package dev.chieppa.ease.platform.time

/**
 * A monotonic clock in nanoseconds. Only loops read it: the simulation sees ticks, never time. Code
 * that needs time takes a clock from its constructor, so tests pass a [ManualClock].
 */
public fun interface Clock {
  /**
   * Nanoseconds since an arbitrary fixed origin; only differences between readings mean anything.
   */
  public fun nanoTime(): Long
}
