package dev.chieppa.ease.net.input

/**
 * Which input packets the host takes, counted in ticks: one whose newest frame is more than [ahead]
 * ticks past the host's tick, or more than [behind] ticks before it, is dropped whole. Clients send
 * the last [redundancy] ticks in every packet.
 */
public class InputWindow(
  public val ahead: Int = DEFAULT_AHEAD,
  public val behind: Int = DEFAULT_BEHIND,
  public val redundancy: Int = DEFAULT_REDUNDANCY,
) {
  init {
    require(ahead >= 0 && behind >= 0) { "A window reaches 0 or more ticks each way" }
    require(redundancy in 1..InputPacket.MAX_FRAMES) {
      "A packet repeats 1 to ${InputPacket.MAX_FRAMES} ticks, not $redundancy"
    }
  }

  public companion object {
    /** Half a second at 60 ticks a second. */
    public const val DEFAULT_AHEAD: Int = 30

    /** Half a second at 60 ticks a second. */
    public const val DEFAULT_BEHIND: Int = 30

    /** Enough to ride out two lost packets in a row. */
    public const val DEFAULT_REDUNDANCY: Int = 3
  }
}
