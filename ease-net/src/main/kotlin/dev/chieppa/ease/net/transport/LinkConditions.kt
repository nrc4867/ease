package dev.chieppa.ease.net.transport

/**
 * The network a loopback link simulates, the same in each direction, counted in ticks: every
 * message waits [latencyTicks], plus up to [jitterTicks] more drawn from the transport's seeded
 * schedule. An unreliable message is lost with chance [loss] and sent twice with chance
 * [duplication]. A reliable message is never lost or repeated: each time the schedule loses it, it
 * arrives one resend later, a round trip of `2 × latencyTicks + 1` ticks, and on the ordered
 * channel the messages behind it wait for it. With [bytesPerTick] above 0, each direction carries
 * that many bytes a tick and messages over it wait their turn, in the order sent.
 */
public class LinkConditions(
  public val latencyTicks: Int = 0,
  public val jitterTicks: Int = 0,
  public val loss: Double = 0.0,
  public val duplication: Double = 0.0,
  public val bytesPerTick: Int = UNCAPPED,
) {
  init {
    require(latencyTicks >= 0) { "Latency is 0 ticks or more, not $latencyTicks" }
    require(jitterTicks >= 0) { "Jitter is 0 ticks or more, not $jitterTicks" }
    require(loss >= 0.0 && loss < 1.0) { "Loss is a chance from 0 up to but not 1, not $loss" }
    require(duplication in 0.0..1.0) { "Duplication is a chance from 0 to 1, not $duplication" }
    require(bytesPerTick >= 0) { "A cap is 0 bytes a tick (none) or more, not $bytesPerTick" }
  }

  /** Ticks a lost reliable message takes to be sent again: one round trip. */
  public val resendTicks: Int
    get() = 2 * latencyTicks + 1

  /** Whether messages arrive the moment they are sent, as on a perfect network. */
  public val isPerfect: Boolean
    get() =
      latencyTicks == 0 &&
        jitterTicks == 0 &&
        loss == 0.0 &&
        duplication == 0.0 &&
        bytesPerTick == UNCAPPED

  public companion object {
    /** The [bytesPerTick] of a link with no bandwidth cap. */
    public const val UNCAPPED: Int = 0

    /** A perfect network: no delay, loss, duplication or cap. */
    public val PERFECT: LinkConditions = LinkConditions()
  }
}
