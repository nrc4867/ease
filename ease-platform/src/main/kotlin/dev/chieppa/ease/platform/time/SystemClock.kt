package dev.chieppa.ease.platform.time

/** The JVM's monotonic clock, for hosts running in real time. */
public object SystemClock : Clock {
  override fun nanoTime(): Long = System.nanoTime()
}
