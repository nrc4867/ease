package dev.chieppa.ease.net.command

/**
 * At most [commands] commands every [ticks] ticks on average, with up to [burst] at once after a
 * quiet spell. Time is counted only in ticks.
 */
public class RateLimit(
  public val commands: Int,
  public val ticks: Int = 1,
  public val burst: Int = commands,
) {
  init {
    require(commands > 0 && ticks > 0) { "A rate is a positive count over positive ticks" }
    require(burst >= 1) { "A burst allows at least one command" }
  }
}
