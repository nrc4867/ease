package dev.chieppa.ease.player

import dev.chieppa.ease.platform.time.Clock
import java.util.Locale

private const val NANOS_PER_MILLI = 1_000_000.0
private const val MILLIS_PER_SECOND = 1_000.0

/**
 * Frame timings from [clock], after the first [skipped] frames: the time between presents, which
 * the display's refresh caps, and the CPU time from the start of a frame to its submission, ticks
 * included.
 */
internal class FrameStats(private val clock: Clock, private val skipped: Int = 10) {
  private var frames = 0
  private var counted = 0
  private var frameStart = 0L
  private var lastPresent = 0L
  private var intervalNanos = 0L
  private var cpuNanos = 0L
  private var cpuMax = 0L

  /** Marks the start of a frame's work. */
  fun startFrame() {
    frameStart = clock.nanoTime()
  }

  /** Marks the frame's commands submitted. */
  fun submitted() {
    val now = clock.nanoTime()
    if (frames >= skipped) {
      val cpu = now - frameStart
      cpuNanos += cpu
      cpuMax = maxOf(cpuMax, cpu)
    }
  }

  /** Marks the frame presented. */
  fun presented() {
    val now = clock.nanoTime()
    if (frames > skipped) {
      intervalNanos += now - lastPresent
      counted++
    }
    lastPresent = now
    frames++
  }

  /** One line: frames between presents and per second, and CPU time per frame. */
  fun summary(): String {
    if (counted == 0) return "Too few frames to time"
    val interval = intervalNanos / NANOS_PER_MILLI / counted
    val cpuMean = cpuNanos / NANOS_PER_MILLI / (counted + 1)
    return String.format(
      Locale.ROOT,
      "Frame time %.2f ms (%.1f fps) over %d frames; CPU %.2f ms mean, %.2f ms max",
      interval,
      MILLIS_PER_SECOND / interval,
      counted,
      cpuMean,
      cpuMax / NANOS_PER_MILLI,
    )
  }
}
