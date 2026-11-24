package dev.chieppa.ease.net.command

/**
 * The limits on what one connection may send the authority: [perConnection] over every command,
 * [perType] over each command type unless [byType] names one by serial name. Each command over a
 * limit is refused and counts a strike; strikes fade by one a tick, and a connection holding more
 * than [strikesToClose] is closed.
 */
public class RateLimits(
  public val perConnection: RateLimit = RateLimit(commands = 8, burst = 32),
  public val perType: RateLimit = RateLimit(commands = 4, burst = 16),
  public val byType: Map<String, RateLimit> = emptyMap(),
  public val strikesToClose: Int = DEFAULT_STRIKES,
) {
  public companion object {
    /** Two seconds of a flood at 60 ticks a second before the connection closes. */
    public const val DEFAULT_STRIKES: Int = 120
  }
}
