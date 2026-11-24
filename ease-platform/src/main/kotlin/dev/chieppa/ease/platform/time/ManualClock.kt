package dev.chieppa.ease.platform.time

/** A clock that moves only when told to, for tests and headless runs that set their own pace. */
public class ManualClock(start: Long = 0) : Clock {
  private var now: Long = start

  override fun nanoTime(): Long = now

  /** Moves the clock forward by [nanos], which may not be negative. */
  public fun advance(nanos: Long) {
    require(nanos >= 0) { "A monotonic clock cannot move back, got $nanos ns" }
    now += nanos
  }
}
