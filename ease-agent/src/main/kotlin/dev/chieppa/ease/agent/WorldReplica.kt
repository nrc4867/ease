package dev.chieppa.ease.agent

import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.runtime.HostedWorld

/**
 * Reproduces a live world in a scratch one, the live one only read: an empty replica world from the
 * same module (see [AgentWorld.replica]), then every logged command, the game's own starting ones
 * included, applied at its tick while the scratch world is stepped, up to the live tick. Any
 * difference left is a change the log does not hold. Rejected commands are recorded in [log].
 */
public class WorldReplica(private val world: AgentWorld, private val log: HostLog) {
  /** A scratch copy of [live] at its tick. Throws [ReplayFailure] when the log cannot get there. */
  public fun of(live: HostedWorld): HostedWorld {
    val scratch = world.replica()
    val logged = live.history.log
    val entries = logged.all
    for (index in entries.indices) {
      val entry = entries[index]
      while (scratch.tick < entry.tick) scratch.step()
      try {
        scratch.execute(logged.commandOf(entry), entry.origin)
      } catch (rejected: CommandRejected) {
        val reason = "Logged command $index at tick ${entry.tick} was rejected: ${rejected.message}"
        log.record(reason)
        throw ReplayFailure(reason)
      }
    }
    while (scratch.tick < live.tick) scratch.step()
    return scratch
  }
}
