package dev.chieppa.ease.net.prediction

/**
 * How a client keeps its lead on the host: its input should reach the host [bufferTicks] before the
 * tick it is for, judged by the earliest of the last [window] reports. While it is off, the client
 * steps [gain] faster or slower per tick of error, at most [maxAdjust] off the host's rate. It
 * jumps ahead only to take its lead at join, or when it has fallen [resyncTicks] behind.
 */
public class ClockSettings(
  public val bufferTicks: Int = DEFAULT_BUFFER_TICKS,
  public val window: Int = DEFAULT_WINDOW,
  public val gain: Double = DEFAULT_GAIN,
  public val maxAdjust: Double = DEFAULT_MAX_ADJUST,
  public val resyncTicks: Int = DEFAULT_RESYNC_TICKS,
) {
  init {
    require(bufferTicks >= 0) { "A buffer is 0 ticks or more, not $bufferTicks" }
    require(window > 0) { "A window holds at least 1 report, not $window" }
    require(gain >= 0.0 && maxAdjust in 0.0..<1.0) { "Adjustments are small and not negative" }
    require(resyncTicks > 0) { "A resync waits for at least 1 tick of error, not $resyncTicks" }
  }

  public companion object {
    /** Two ticks to spare: one for jitter, one for the step the host is already in. */
    public const val DEFAULT_BUFFER_TICKS: Int = 2

    /** The last 8 reports, about an eighth of a second. */
    public const val DEFAULT_WINDOW: Int = 8

    /** 2% faster or slower per tick of error. */
    public const val DEFAULT_GAIN: Double = 0.02

    /** At most 5% off the host's rate. */
    public const val DEFAULT_MAX_ADJUST: Double = 0.05

    /** A quarter second at 60 ticks a second. */
    public const val DEFAULT_RESYNC_TICKS: Int = 15
  }
}
