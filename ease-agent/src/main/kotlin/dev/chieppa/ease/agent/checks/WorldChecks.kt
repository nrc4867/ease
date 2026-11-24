package dev.chieppa.ease.agent.checks

import dev.chieppa.ease.agent.AgentWorld
import dev.chieppa.ease.agent.HostLog
import dev.chieppa.ease.agent.ReplayFailure
import dev.chieppa.ease.agent.WorldReplica
import dev.chieppa.ease.runtime.HostedWorld

/**
 * The headless checks, each run on the world's thread against a scratch copy that [WorldReplica]
 * makes of the live world, so the live world is only read.
 */
public class WorldChecks(private val world: AgentWorld, private val log: HostLog) {
  private val replica = WorldReplica(world, log)

  /**
   * Whether replaying the live log onto a fresh world, at the same ticks and for as many ticks,
   * gives the live world exactly.
   */
  public suspend fun replay(): CheckResult =
    onCopy(REPLAY) { live, copy ->
      val logged = live.history.log.all
      log.record("Replay: ${logged.size} logged commands to tick ${copy.tick}")
      val failures =
        WorldDiff()
          .between(
            live.codec.worldToJson(live.world),
            copy.codec.worldToJson(copy.world),
            LIVE,
            REPLAYED,
          )
      CheckResult(REPLAY, failures.isEmpty(), copy.tick, failures)
    }

  /** Whether a scratch copy of the live world runs [ticks] more ticks without throwing. */
  @Suppress(
    "TooGenericExceptionCaught"
  ) // A game's systems may throw anything; the check reports it.
  public suspend fun ticks(ticks: Int): CheckResult =
    onCopy(TICKS) { _, copy ->
      val start = copy.tick
      val failure =
        try {
          repeat(ticks) { copy.step() }
          null
        } catch (thrown: Exception) {
          "Tick ${copy.tick} threw ${thrown.javaClass.name}: ${thrown.message}"
        }
      log.record(
        "Ticks: ran ${copy.tick - start} of $ticks on a scratch copy, to tick ${copy.tick}"
      )
      failure?.let(log::record)
      CheckResult(TICKS, failure == null, copy.tick, listOfNotNull(failure))
    }

  /**
   * Runs [check] on the world's thread with the live world and a scratch copy of it; a copy that
   * cannot be made fails the check with the reason.
   */
  private suspend fun onCopy(
    name: String,
    check: (HostedWorld, HostedWorld) -> CheckResult,
  ): CheckResult = world.access { live ->
    val copy =
      try {
        replica.of(live)
      } catch (failure: ReplayFailure) {
        return@access CheckResult(name, false, live.tick, listOf(failure.message.orEmpty()))
      }
    check(live, copy)
  }

  public companion object {
    /** The replay check's name. */
    public const val REPLAY: String = "replay"

    /** The scratch-ticks check's name. */
    public const val TICKS: String = "ticks"

    private const val LIVE = "live"
    private const val REPLAYED = "replayed"
  }
}
